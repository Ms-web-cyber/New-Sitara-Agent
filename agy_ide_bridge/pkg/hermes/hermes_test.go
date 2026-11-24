package hermes

import "testing"

func TestJournalUndoAndPolicy(t *testing.T) {
	m := NewManager(t.TempDir())
	if err := m.SetPolicy(Policy{Enabled: true, AllowedCommands: []string{" tests ", "tests"}}); err != nil { t.Fatal(err) }
	if p := m.Policy(); !p.Enabled || len(p.AllowedCommands) != 1 || p.AllowedCommands[0] != "tests" { t.Fatalf("bad policy: %+v", p) }
	if m.Allowed("files") || m.Allowed("root") { t.Fatal("deny-by-default policy failed") }
	if err := m.SetPolicy(Policy{Enabled: true, AllowedCapabilities: []string{"files", "root"}}); err != nil { t.Fatal(err) }
	if !m.Allowed("files") || m.Allowed("root") { t.Fatal("hard safety gate failed") }
	e, err := m.Record("action", "Updated a reversible prompt", "/work", "checkpoint-1")
	if err != nil { t.Fatal(err) }
	u, err := m.Undo(e.ID)
	if err != nil || !u.Undone { t.Fatalf("undo: %+v %v", u, err) }
	if _, err := m.Record("bad", "no", "", ""); err == nil { t.Fatal("invalid kind was accepted") }
}
