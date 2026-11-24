// Package hermes manages Sitara's reversible, policy-controlled self-improvement work.
package hermes

import (
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"errors"
	"net/http"
	"os"
	"path/filepath"
	"sort"
	"strings"
	"sync"
	"time"
)

const maxJournal = 2000

// Policy is intentionally deny-by-default for actions which can escape a project.
type Policy struct {
	Enabled          bool     `json:"enabled"`
	Continuous       bool     `json:"continuous"`
	BackendSync      bool     `json:"backend_sync"`
	AllowedRoots     []string `json:"allowed_roots"`
	AllowedCommands  []string `json:"allowed_commands"`
	AllowedDomains   []string `json:"allowed_domains"`
	AllowedCapabilities []string `json:"allowed_capabilities"`
}

type Event struct {
	ID string `json:"id"`
	Kind string `json:"kind"` // learning | action | risk | rollback
	Summary string `json:"summary"`
	Project string `json:"project,omitempty"`
	At int64 `json:"at"`
	UndoData string `json:"undo_data,omitempty"`
	Undone bool `json:"undone,omitempty"`
}

type state struct { Policy Policy `json:"policy"`; Events []Event `json:"events"` }

type Manager struct { path string; mu sync.Mutex; state state }

func NewManager(home string) *Manager {
	m := &Manager{path: filepath.Join(home, ".sitara_bridge", "hermes", "state.json")}
	m.load()
	return m
}

func (m *Manager) load() {
	b, err := os.ReadFile(m.path); if err == nil { _ = json.Unmarshal(b, &m.state) }
}
func (m *Manager) saveLocked() error {
	if len(m.state.Events) > maxJournal { m.state.Events = m.state.Events[len(m.state.Events)-maxJournal:] }
	b, err := json.MarshalIndent(m.state, "", "  "); if err != nil { return err }
	if err := os.MkdirAll(filepath.Dir(m.path), 0700); err != nil { return err }
	tmp := m.path + ".tmp"; if err := os.WriteFile(tmp, b, 0600); err != nil { return err }; return os.Rename(tmp, m.path)
}
func (m *Manager) Policy() Policy { m.mu.Lock(); defer m.mu.Unlock(); return m.state.Policy }
func (m *Manager) SetPolicy(p Policy) error { m.mu.Lock(); defer m.mu.Unlock(); m.state.Policy = cleanPolicy(p); return m.saveLocked() }
func cleanPolicy(p Policy) Policy { p.AllowedRoots=clean(p.AllowedRoots); p.AllowedCommands=clean(p.AllowedCommands); p.AllowedDomains=clean(p.AllowedDomains); p.AllowedCapabilities=clean(p.AllowedCapabilities); return p }
func clean(v []string) []string { out:=make([]string,0,len(v)); seen:=map[string]bool{}; for _,x:=range v { x=strings.TrimSpace(x); if x!=""&&!seen[x] { seen[x]=true; out=append(out,x) } }; sort.Strings(out); return out }

// Record accepts only descriptive outcome data. Tool execution remains with the existing agent runners.
func (m *Manager) Record(kind, summary, project, undo string) (Event, error) {
	if kind!="learning"&&kind!="action"&&kind!="risk"&&kind!="rollback" { return Event{}, errors.New("invalid Hermes event kind") }
	summary=strings.TrimSpace(summary); if summary=="" || len(summary)>4000 { return Event{}, errors.New("invalid Hermes summary") }
	m.mu.Lock(); defer m.mu.Unlock(); now:=time.Now().UnixMilli(); sum:=sha256.Sum256([]byte(kind+summary+project+strconv64(now))); e:=Event{ID:hex.EncodeToString(sum[:8]),Kind:kind,Summary:summary,Project:strings.TrimSpace(project),At:now,UndoData:undo}; m.state.Events=append(m.state.Events,e); return e,m.saveLocked()
}
func strconv64(v int64) string { return time.UnixMilli(v).UTC().Format(time.RFC3339Nano) }
func (m *Manager) Events() []Event { m.mu.Lock(); defer m.mu.Unlock(); out:=append([]Event(nil),m.state.Events...); for i,j:=0,len(out)-1;i<j;i,j=i+1,j-1 { out[i],out[j]=out[j],out[i] }; return out }
// Allowed is the shared gate future Hermes tool runners must call before an action.
// High-impact capabilities are never eligible for automatic execution.
func (m *Manager) Allowed(capability string) bool { capability=strings.TrimSpace(capability); if capability=="payments"||capability=="credentials"||capability=="root"||capability=="release"||capability=="install" { return false }; p:=m.Policy(); if !p.Enabled{return false}; for _,v:=range p.AllowedCapabilities { if v==capability{return true} }; return false }
func (m *Manager) Undo(id string) (Event,error) { m.mu.Lock(); defer m.mu.Unlock(); for i:=len(m.state.Events)-1;i>=0;i-- { if m.state.Events[i].ID==id { if m.state.Events[i].UndoData=="" { return Event{},errors.New("this event has no reversible checkpoint") }; m.state.Events[i].Undone=true; return m.state.Events[i],m.saveLocked() } }; return Event{},errors.New("Hermes event not found") }

func (m *Manager) ServeHTTP(w http.ResponseWriter,r *http.Request) { route:=strings.Trim(strings.TrimPrefix(r.URL.Path,"/api/hermes"),"/"); write:=func(v any,err error){ w.Header().Set("Content-Type","application/json"); o:=map[string]any{"success":err==nil}; if err!=nil{o["error"]=err.Error()}; if v!=nil{o["data"]=v}; _=json.NewEncoder(w).Encode(o) }; switch {
	case route=="status"&&r.Method==http.MethodGet: write(map[string]any{"policy":m.Policy(),"events":m.Events()},nil)
	case route=="policy"&&r.Method==http.MethodPost: var p Policy; if err:=json.NewDecoder(http.MaxBytesReader(w,r.Body,1<<20)).Decode(&p);err!=nil{write(nil,err);return}; write(m.Policy(),m.SetPolicy(p))
	case route=="event"&&r.Method==http.MethodPost: var q struct{ Kind string `json:"kind"`; Summary string `json:"summary"`; Project string `json:"project"`; UndoData string `json:"undo_data"` }; if err:=json.NewDecoder(http.MaxBytesReader(w,r.Body,1<<20)).Decode(&q);err!=nil{write(nil,err);return}; e,err:=m.Record(q.Kind,q.Summary,q.Project,q.UndoData);write(e,err)
	case route=="undo"&&r.Method==http.MethodPost: var q struct{ID string `json:"id"`}; if err:=json.NewDecoder(http.MaxBytesReader(w,r.Body,1<<16)).Decode(&q);err!=nil{write(nil,err);return}; e,err:=m.Undo(q.ID);write(e,err)
	case route=="authorize"&&r.Method==http.MethodPost: var q struct{Capability string `json:"capability"`}; if err:=json.NewDecoder(http.MaxBytesReader(w,r.Body,1<<16)).Decode(&q);err!=nil{write(nil,err);return}; write(map[string]bool{"allowed":m.Allowed(q.Capability)},nil)
	default: http.Error(w,"unknown Hermes endpoint",http.StatusNotFound)
	} }
