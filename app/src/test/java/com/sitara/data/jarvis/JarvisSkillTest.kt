package com.sitara.data.jarvis

import org.junit.Assert.assertTrue
import org.junit.Test

class JarvisSkillTest {
    @Test fun voiceGuideKeepsSitaraAnAiCompanion() {
        val guide = JarvisSkill.guide()
        assertTrue(guide.contains("You are Sitara, the AI voice companion"))
        assertTrue(guide.contains("Hindi-Hinglish"))
        assertTrue(guide.contains("not a human woman"))
        assertTrue(guide.contains("never initiate personal check-ins"))
    }

    @Test fun commandGuideUsesSitaraIdentity() {
        assertTrue(JarvisSkill.commandGuide().contains("You are Sitara, the AI companion"))
    }
}
