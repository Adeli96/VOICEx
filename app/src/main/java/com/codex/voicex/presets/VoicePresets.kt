package com.codex.voicex.presets

import com.codex.voicex.dsp.DspParams

/**
 * VoiceX preset architecture — Made by CODEx Team.
 *
 * Categories and presets are pure DATA. The UI never hard-codes effects:
 * it only renders this catalog and passes DspParams to the offline DSP engine.
 * Adding hundreds of new presets = adding new lines here (or user-saved
 * custom presets from PresetStore) — no UI/engine rewrites.
 *
 * Age labels (e.g. "Girl 18") are approximate STYLISTIC presets, not a claim
 * about a real person's biological age. Animal presets are DSP approximations
 * of animal character, not scientifically accurate animal synthesis.
 */

data class VoiceCategory(val id: String, val icon: String, val en: String, val fa: String)

data class VoicePreset(
    val id: String,
    val catId: String,
    val en: String,
    val fa: String,
    val params: DspParams,
    val custom: Boolean = false
) {
    fun name(isFa: Boolean) = if (isFa) fa else en
}

object VoicePresets {

    private fun c(id: String, icon: String, en: String, fa: String) = VoiceCategory(id, icon, en, fa)

    val categories: List<VoiceCategory> = listOf(
        c("girl", "👩", "Girls & Women", "دختر و زن"),
        c("man", "🧔", "Men", "مرد"),
        c("animal", "🐾", "Animals", "حیوان"),
        c("robot", "🤖", "Robots & Space", "ربات و فضا"),
        c("fantasy", "🪄", "Fantasy", "فانتزی"),
        c("space", "🏛️", "Places & Rooms", "محیط"),
        c("comms", "📡", "Communication", "ارتباطات"),
        c("fx", "🎛️", "Effects", "افکت"),
        c("custom", "⭐", "Custom Voice", "صدای سفارشی")
    )

    private fun v(
        cat: String, id: String, en: String, fa: String,
        pitch: Float = 0f, formant: Float = 0f, speed: Float = 1f,
        bass: Float = 0f, treble: Float = 0f, lp: Int = 0,
        drive: Float = 0f, bits: Int = 16, dsm: Int = 1,
        ring: Float = 0f, trHz: Float = 0f, trD: Float = 0f,
        chorus: Float = 0f, flanger: Float = 0f, phaser: Float = 0f,
        dMs: Int = 0, dFb: Float = 0f, dMix: Float = 0f,
        rev: Float = 0f, noise: Float = 0f, gate: Float = 0f, vol: Float = 1f,
        ringMix: Float = 1f
    ) = VoicePreset(
        "${cat}_$id", cat, en, fa,
        DspParams(pitch, formant, speed, bass, treble, lp, drive, bits, dsm, ring,
            trHz, trD, chorus, flanger, phaser, dMs, dFb, dMix, rev, noise, gate, vol,
            ringMix = ringMix)
    )

    val presets: List<VoicePreset> = listOf(
        // ── girl ──
        v("girl","woman_bright","Bright Girl","دختر روشن", pitch=4.0f, formant=4.0f, treble=2.0f),
        v("girl","girl_soft","Soft Girl","دختر لطیف", pitch=3.0f, formant=3.5f, treble=1.0f, lp=9000, rev=0.2f),
        v("girl","girl_sweet","Sweet Girl","دختر شیرین", pitch=4.5f, formant=4.0f, treble=3.0f, chorus=0.15f),
        v("girl","girl_cool","Cool Girl","دختر خونسرد", pitch=2.0f, formant=2.0f, bass=1.0f, treble=1.0f, lp=8000),
        v("girl","woman","Adult Woman","زن بالغ", pitch=2.5f, formant=2.5f),
        v("girl","woman_deep","Deep Woman","زنانه بم", pitch=-1.5f, formant=-1.5f, bass=3.0f),
        v("girl","woman_mature","Mature Lady","بانوی خوش‌صدا", pitch=1.0f, formant=1.5f, bass=2.0f, treble=1.0f),
        v("girl","girl_host","Female Host","گوینده‌ی خانم", pitch=2.5f, formant=2.5f, bass=2.0f, treble=2.0f, rev=0.2f, gate=0.2f),
        v("girl","girl_whisper","Whisperer","نجواگر", pitch=3.0f, formant=3.0f, treble=3.0f, lp=6500, rev=0.35f, noise=0.05f, gate=0.1f),
        v("girl","girl_pop","Pop Singer","خواننده‌ی پاپ", pitch=3.5f, formant=3.0f, treble=3.0f, chorus=0.4f, rev=0.45f),
        v("girl","girl_diva","Diva","دیوا", pitch=2.0f, formant=3.0f, bass=-1.0f, treble=2.0f, chorus=0.25f, rev=0.525f),
        v("girl","girl_phone","Lady on Phone","خانم پشت تلفن", pitch=3.0f, formant=3.0f, bass=-8.0f, lp=3400, bits=12),
        v("girl","girl_dream","Dreamy","رویایی", pitch=3.5f, formant=3.0f, chorus=0.5f, rev=0.625f),
        v("girl","elder_f","Old Woman","پیرزن", pitch=1.0f, formant=1.0f, treble=-3.0f, lp=4800, trHz=5.0f, trD=0.2f),
        // ── man ──
        v("man","man_young","Young Man","مرد جوان", pitch=-2.5f, formant=-2.0f, bass=2.0f),
        v("man","man_deep","Deep Man","مردانه بم", pitch=-5.0f, formant=-4.0f, bass=5.0f),
        v("man","man_giant","Giant Voice","غول‌صدا", pitch=-9.0f, formant=-7.0f, bass=7.0f, drive=0.2f),
        v("man","narrator","Male Narrator","گوینده‌ی آقا", pitch=-1.5f, formant=-1.0f, bass=3.0f, treble=2.0f, rev=0.21f, gate=0.2f),
        v("man","man_smooth","Warm Man","مرد گرم", pitch=-2.0f, formant=-1.0f, bass=3.0f, lp=9000, rev=0.21f),
        v("man","man_rough","Rough Man","مرد خشن", pitch=-3.5f, formant=-3.0f, bass=3.0f, drive=0.25f),
        v("man","man_trailer","Movie Trailer","تیزر سینمایی", pitch=-6.0f, formant=-4.0f, bass=6.0f, rev=0.6f, gate=0.15f),
        v("man","man_radio","Radio Host","مجری رادیو", pitch=-2.0f, formant=-1.5f, bass=4.0f, treble=1.0f, rev=0.2f, gate=0.2f),
        v("man","man_phone","Man on Phone","آقا پشت تلفن", pitch=-2.0f, bass=-9.0f, lp=3300, drive=0.1f, bits=12),
        v("man","man_sergeant","Officer","افسر", pitch=-4.0f, formant=-3.0f, treble=2.0f, drive=0.3f, dMs=120, dFb=0.3f, dMix=0.15f),
        v("man","elder","Old Man","پیرمرد", pitch=-2.5f, formant=-1.0f, treble=-3.0f, lp=4800, trHz=5.0f, trD=0.2f, noise=0.01f),
        v("man","man_calm","Calm Man","مرد آرام", pitch=-1.5f, formant=-1.0f, bass=2.0f, lp=7000),
        v("man","man_tenor","Tenor","تنور", pitch=-1.0f, treble=1.0f, chorus=0.25f, rev=0.425f),
        // ── animal ──
        v("animal","cat","Cat","گربه", pitch=9.0f, formant=5.0f, treble=3.0f, trHz=7.0f, trD=0.25f),
        v("animal","dog","Big Dog","سگ بزرگ", pitch=-6.0f, formant=-4.0f, bass=4.0f, drive=0.3f),
        v("animal","bird","Bird","پرنده", pitch=12.0f, formant=6.0f, treble=4.0f, trHz=9.0f, trD=0.5f),
        v("animal","lion","Lion","شیر", pitch=-9.0f, formant=-5.0f, bass=8.0f, drive=0.4f, rev=0.375f),
        v("animal","mouse","Mouse","موش", pitch=11.0f, formant=8.0f, treble=4.0f),
        v("animal","cow","Cow","گاو", pitch=-7.0f, formant=-3.0f, bass=6.0f, trHz=2.5f, trD=0.2f),
        v("animal","sheep","Sheep","گوسفند", pitch=3.0f, trHz=6.5f, trD=0.55f),
        v("animal","wolf","Wolf","گرگ", pitch=-5.0f, formant=-2.0f, drive=0.15f, rev=0.6f),
        v("animal","frog","Frog","قورباغه", pitch=-4.0f, formant=2.0f, drive=0.2f, trHz=12.0f, trD=0.6f),
        v("animal","elephant","Elephant","فیل", pitch=-11.0f, formant=-5.0f, bass=8.0f, drive=0.2f, rev=0.55f),
        v("animal","bee","Bee","زنبور", pitch=5.0f, treble=3.0f, ring=90.0f, ringMix=0.5f),
        v("animal","duck","Duck","اردک", pitch=3.0f, treble=4.0f, lp=3500, drive=0.3f, trHz=14.0f, trD=0.4f),
        v("animal","horse","Horse","اسب", pitch=-3.0f, bass=3.0f, drive=0.15f, trHz=8.0f, trD=0.35f),
        v("animal","snake","Snake","مار", pitch=-3.0f, treble=9.0f, chorus=0.4f, noise=0.12f, gate=0.2f),
        v("animal","bear","Bear","خرس", pitch=-8.0f, formant=-5.0f, bass=7.0f, drive=0.35f),
        v("animal","dolphin","Dolphin","دلفین", pitch=10.0f, formant=4.0f, trHz=16.0f, trD=0.5f, rev=0.55f),
        v("animal","owl","Owl","جغد", pitch=-1.0f, lp=3500, trHz=3.0f, trD=0.4f, rev=0.65f),
        // ── robot ──
        v("robot","robot","Robot","ربات", drive=0.15f, ring=50.0f, ringMix=0.8f, dMs=30, dFb=0.4f, dMix=0.3f),
        v("robot","cyborg","Cyborg","سایبورگ", pitch=-3.0f, drive=0.25f, ring=30.0f, ringMix=0.6f, dMs=55, dFb=0.35f, dMix=0.35f),
        v("robot","assistant","Virtual Assistant","دستیار مجازی", pitch=1.0f, bits=11, dsm=2, ring=110.0f, ringMix=0.35f, chorus=0.3f),
        v("robot","alien","Alien","بیگانه", pitch=6.0f, ring=70.0f, ringMix=0.7f, chorus=0.6f, rev=0.425f),
        v("robot","astro","Astronaut","فضانورد", bass=-6.0f, lp=3600, dMs=380, dFb=0.5f, dMix=0.3f, rev=0.7f, noise=0.04f),
        v("robot","dark","Dark Commander","فرمانده تاریک", pitch=-6.0f, formant=-5.0f, lp=4000, drive=0.2f, ring=20.0f, ringMix=0.2f, rev=0.3f),
        v("robot","old_android","Old Android","اندروید قدیمی", bits=7, dsm=3, ring=80.0f, ringMix=0.9f),
        v("robot","cold_ai","Cold AI","هوش مصنوعی سرد", pitch=-1.0f, lp=7000, ring=140.0f, ringMix=0.25f, dMs=60, dFb=0.35f, dMix=0.15f, rev=0.275f),
        v("robot","war_bot","War Bot","ربات جنگی", pitch=-6.0f, bass=6.0f, drive=0.4f, ring=35.0f, ringMix=0.7f),
        v("robot","kind_bot","Kind Bot","ربات مهربان", pitch=3.0f, bits=12, dsm=2, ring=200.0f, ringMix=0.3f, chorus=0.3f),
        v("robot","bit8","8-bit Computer","کامپیوتر ۸ بیتی", lp=6000, drive=0.1f, bits=5, dsm=6),
        v("robot","mothership","Mothership","سفینه‌ی مادر", pitch=-4.0f, ring=15.0f, ringMix=0.2f, chorus=0.4f, dMs=500, dFb=0.5f, dMix=0.3f, rev=0.85f),
        v("robot","galaxy_msg","Galaxy Message","پیام از کهکشان", pitch=2.0f, bass=-4.0f, lp=2600, dMs=700, dFb=0.55f, dMix=0.35f, rev=0.725f, noise=0.07f),
        // ── fantasy ──
        v("fantasy","wizard","Wizard","جادوگر", pitch=-3.0f, chorus=0.35f, rev=0.55f),
        v("fantasy","fairy","Fairy","پری", pitch=5.0f, formant=4.0f, treble=3.0f, chorus=0.7f, rev=0.65f),
        v("fantasy","ghost","Ghost","روح", pitch=-2.0f, lp=3200, trHz=4.0f, trD=0.3f, dMs=300, dFb=0.5f, dMix=0.35f, rev=0.9f),
        v("fantasy","zombie","Zombie","زامبی", pitch=-5.0f, lp=4500, drive=0.35f, bits=10, dsm=2, trHz=3.0f, trD=0.3f),
        v("fantasy","monster","Monster","هیولا", pitch=-10.0f, formant=-6.0f, bass=8.0f, drive=0.5f, rev=0.35f),
        v("fantasy","demon","Demon","اهریمن", pitch=-12.0f, drive=0.6f, ring=25.0f, ringMix=0.35f, chorus=0.4f, rev=0.525f),
        v("fantasy","dragon","Dragon","اژدها", pitch=-8.0f, formant=-6.0f, bass=8.0f, drive=0.4f, trHz=3.0f, trD=0.2f, rev=0.6f),
        v("fantasy","vampire","Vampire","خون‌آشام", pitch=-3.0f, formant=-2.0f, bass=3.0f, lp=6000, chorus=0.2f, rev=0.525f),
        v("fantasy","ogre","Ogre","غول", pitch=-7.0f, formant=-4.0f, bass=5.0f, drive=0.3f, trHz=4.0f, trD=0.2f),
        v("fantasy","elf","Elf","الف", pitch=4.0f, formant=3.5f, treble=3.0f, chorus=0.5f, rev=0.6f),
        v("fantasy","genie","Genie","غول چراغ جادو", pitch=-4.0f, chorus=0.4f, dMs=180, dFb=0.5f, dMix=0.3f, rev=0.75f),
        v("fantasy","witch","Witch","ساحره", pitch=3.0f, lp=5500, drive=0.2f, trHz=6.0f, trD=0.25f, rev=0.45f),
        v("fantasy","skeleton","Skeleton","اسکلت", pitch=-2.0f, lp=4000, bits=9, dsm=2, dMs=150, dFb=0.4f, dMix=0.3f, rev=0.5f),
        v("fantasy","angel","Angel","فرشته", pitch=3.0f, treble=3.0f, chorus=0.8f, rev=0.825f),
        // ── space ──
        v("space","cave","Cave","غار", dMs=220, dFb=0.4f, dMix=0.2f, rev=0.85f),
        v("space","tunnel","Tunnel","تونل", bass=2.0f, dMs=95, dFb=0.55f, dMix=0.5f, rev=0.425f),
        v("space","underwater","Underwater","زیر آب", lp=1100, trHz=2.2f, trD=0.3f, chorus=0.7f, rev=0.45f),
        v("space","stadium","Stadium","ورزشگاه", drive=0.1f, dMs=420, dFb=0.35f, dMix=0.35f, rev=0.725f),
        v("space","bath","Bathroom","حمام", treble=3.0f, rev=0.475f),
        v("space","hall","Great Hall","تالار بزرگ", treble=1.0f, rev=0.84f),
        v("space","mountain","Mountain","کوهستان", dMs=650, dFb=0.45f, dMix=0.45f, rev=0.55f),
        v("space","forest","Forest","جنگل", lp=7000, chorus=0.2f, rev=0.55f),
        v("space","alley","Empty Alley","کوچه‌ی خالی", lp=8000, dMs=140, dFb=0.35f, dMix=0.3f, rev=0.4f),
        v("space","small_room","Small Room","اتاق کوچک", bass=1.0f, rev=0.165f),
        v("space","behind_wall","Behind a Wall","پشت دیوار", bass=3.0f, lp=900, drive=0.05f, rev=0.25f),
        v("space","well","Deep Well","چاه عمیق", lp=3500, dMs=210, dFb=0.6f, dMix=0.5f, rev=0.75f),
        v("space","airport","Airport","فرودگاه", bass=-5.0f, lp=5000, drive=0.1f, dMs=350, dFb=0.3f, dMix=0.3f, rev=0.7f, noise=0.02f),
        // ── comms ──
        v("comms","radio","Old Radio","رادیو قدیمی", bass=-9.0f, lp=3400, drive=0.25f, bits=12, noise=0.03f),
        v("comms","phone","Telephone","تلفن", bass=-12.0f, treble=2.0f, lp=3200, drive=0.15f, bits=10),
        v("comms","walkie","Walkie-Talkie","بی‌سیم", bass=-10.0f, lp=2800, drive=0.5f, bits=9, dsm=2, noise=0.06f, gate=0.3f),
        v("comms","mega","Megaphone","بلندگو", bass=-6.0f, treble=3.0f, lp=5000, drive=0.6f, dMs=120, dFb=0.3f, dMix=0.2f),
        v("comms","badnet","Bad Connection","اینترنت ضعیف", lp=4000, bits=6, dsm=5, noise=0.04f, gate=0.5f),
        v("comms","satcall","Satellite Call","تماس ماهواره‌ای", bass=-8.0f, lp=3000, bits=11, dsm=2, dMs=550, dFb=0.4f, dMix=0.3f, noise=0.05f),
        v("comms","cheap_mic","Cheap Mic","میکروفن ارزان", bass=-7.0f, treble=2.0f, lp=5500, drive=0.2f, noise=0.02f),
        v("comms","voice_msg","Voice Message","پیام صوتی", bass=-6.0f, lp=6500, bits=12, gate=0.25f),
        v("comms","intercom","Intercom","اینترکام", bass=-10.0f, lp=3600, drive=0.4f, noise=0.03f),
        v("comms","old_recorder","Old Recorder","ضبط‌صوت قدیمی", bass=-4.0f, lp=3800, drive=0.15f, bits=11, trHz=0.8f, trD=0.1f, noise=0.05f),
        v("comms","announce","Public Announcement","اعلان عمومی", bass=-6.0f, lp=4200, drive=0.25f, dMs=300, dFb=0.4f, dMix=0.35f, rev=0.6f),
        v("comms","police","Police Radio","بی‌سیم پلیس", bass=-11.0f, lp=2700, drive=0.6f, bits=9, noise=0.07f, gate=0.4f),
        v("comms","bad_phones","Broken Headphones","هدفون خراب", bass=-3.0f, lp=2200, drive=0.3f, noise=0.06f, gate=0.3f),
        // ── fx ──
        v("fx","lofi","Lo-Fi","لوفای", lp=4500, drive=0.1f, bits=8, dsm=4, noise=0.04f),
        v("fx","studio","Studio","استودیویی", bass=2.0f, treble=2.5f, rev=0.225f, gate=0.15f),
        v("fx","echo","Echo","اکو", dMs=280, dFb=0.5f, dMix=0.55f),
        v("fx","tremolo","Tremolo","ترمولو", trHz=6.0f, trD=0.6f),
        v("fx","oct_dn","Octave Down","یک اکتاو پایین", pitch=-12.0f),
        v("fx","oct_up","Octave Up","یک اکتاو بالا", pitch=12.0f),
        v("fx","choir","Choir","گروه‌خوان", chorus=1.0f, rev=0.35f),
        v("fx","helium","Helium","هلیومی", pitch=8.0f, formant=7.0f, treble=3.0f),
        v("fx","disco","Disco","دیسکو", treble=2.0f, chorus=0.6f, dMs=180, dFb=0.5f, dMix=0.35f, rev=0.45f),
        v("fx","shaky","Shaky","لرزان", trHz=12.0f, trD=0.8f),
        v("fx","gramophone","Gramophone","گرامافون", bass=-6.0f, lp=3000, drive=0.3f, bits=10, noise=0.06f),
        v("fx","bitcrusher","Bitcrusher","بیت‌کراشر", bits=4, dsm=8),
        v("fx","heavy","Heavy Distortion","اغتشاش شدید", bass=3.0f, lp=6000, drive=1.0f),
        v("fx","fifth_up","Fifth Up","پنجم بالا", pitch=7.0f),
        v("fx","fifth_dn","Fifth Down","پنجم پایین", pitch=-7.0f)
    )

    fun byCategory(catId: String): List<VoicePreset> = presets.filter { it.catId == catId }

    fun byId(id: String): VoicePreset? = presets.firstOrNull { it.id == id }

    fun category(id: String): VoiceCategory? = categories.firstOrNull { it.id == id }

    /** Global search across all categories, both languages. */
    fun search(query: String, customPresets: List<VoicePreset> = emptyList()): List<VoicePreset> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return (presets + customPresets).filter {
            it.en.lowercase().contains(q) || it.fa.contains(q) || it.id.lowercase().contains(q)
        }
    }
}
