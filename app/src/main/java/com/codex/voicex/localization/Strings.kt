package com.codex.voicex.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

/**
 * VoiceX localization — Persian (default) + English.
 * All UI strings live here so adding a new language later only requires
 * adding one more map — no UI rewrites.  Made by CODEx Team.
 */
object Strings {

    // key -> Pair(English, Persian)
    private val map: Map<String, Pair<String, String>> = mapOf(
        "app_name"            to ("VoiceX" to "VoiceX"),
        "subtitle"            to ("Offline Voice Changer" to "تغییر دهنده صدای آفلاین"),
        "made_by"             to ("Made by CODEx Team" to "ساخته شده توسط تیم CODEx"),
        "made_by_short"       to ("Made by CODEx" to "ساخته توسط CODEx"),

        // Home
        "home_hello"          to ("Welcome" to "خوش آمدید"),
        "home_tagline"        to ("Record. Transform. Play." to "ضبط. تغییر. پخش."),
        "home_record"         to ("Record" to "ضبط"),
        "home_recordings"     to ("Recordings" to "ضبط‌ها"),
        "home_library"        to ("Voice Library" to "کتابخانه صداها"),
        "home_favorites"      to ("Favorites" to "علاقه‌مندی‌ها"),
        "home_recent"         to ("Recent Effects" to "افکت‌های اخیر"),
        "home_settings"       to ("Settings" to "تنظیمات"),
        "home_about"          to ("About" to "درباره"),
        "home_tap_record"     to ("Tap the big button to start recording" to "برای شروع ضبط، دکمه بزرگ را لمس کنید"),

        // Recorder
        "rec_title"           to ("New Recording" to "ضبط جدید"),
        "rec_start"           to ("Start" to "شروع"),
        "rec_pause"           to ("Pause" to "توقف"),
        "rec_resume"          to ("Resume" to "ادامه"),
        "rec_stop"            to ("Stop" to "پایان"),
        "rec_cancel"          to ("Cancel" to "لغو"),
        "rec_save"            to ("Save" to "ذخیره"),
        "rec_ready_choose"    to ("Recording saved. Choose a voice to transform." to "ضبط ذخیره شد. یک صدا برای تغییر انتخاب کنید."),
        "rec_choose_voice"    to ("Choose a voice" to "انتخاب صدا"),
        "rec_processing"      to ("Processing voice..." to "در حال پردازش صدا..."),
        "rec_ready"           to ("Voice ready" to "صدا آماده است"),
        "rec_play"            to ("Play" to "پخش"),
        "rec_process_again"   to ("Process again" to "پردازش مجدد"),
        "rec_share"           to ("Share" to "اشتراک"),
        "rec_favorite"        to ("Favorite" to "علاقه‌مندی"),
        "rec_unfavorite"      to ("Unfavorite" to "حذف از علاقه‌مندی"),

        // Library
        "lib_title"           to ("Voice Library" to "کتابخانه صداها"),
        "lib_search"          to ("Search voices" to "جستجوی صداها"),
        "lib_empty_search"    to ("No voices match your search" to "صدایی با جستجوی شما پیدا نشد"),
        "lib_category_count"  to ("presets" to "پریست"),
        "lib_all"             to ("All Categories" to "همه دسته‌ها"),
        "lib_show_favorites"  to ("Show favorites section" to "نمایش بخش علاقه‌مندی‌ها"),

        // Recordings
        "recs_title"          to ("Recordings" to "ضبط‌ها"),
        "recs_empty"          to ("No recordings yet. Tap record to start." to "هنوز ضبطی وجود ندارد. برای شروع، دکمه ضبط را لمس کنید."),
        "recs_original"       to ("Original" to "اصلی"),
        "recs_processed"      to ("Processed" to "پردازش‌شده"),
        "recs_rename"         to ("Rename" to "تغییر نام"),
        "recs_delete"         to ("Delete" to "حذف"),
        "recs_duplicate"      to ("Duplicate" to "کپی"),
        "recs_share"          to ("Share" to "اشتراک"),
        "recs_apply_voice"    to ("Apply Voice" to "اعمال صدا"),
        "recs_new_name"       to ("New name" to "نام جدید"),
        "recs_confirm_delete" to ("Delete this recording?" to "این ضبط حذف شود؟"),
        "recs_ok"             to ("OK" to "تایید"),
        "recs_cancel"         to ("Cancel" to "لغو"),

        // Favorites
        "fav_title"           to ("Favorites" to "علاقه‌مندی‌ها"),
        "fav_empty"           to ("No favorite voices yet. Tap the heart on any voice to save it." to "هیچ صدای علاقه‌مندی ثبت نشده است. برای افزودن، آیکون قلب هر صدا را لمس کنید."),

        // Recent
        "recent_title"        to ("Recent Effects" to "افکت‌های اخیر"),
        "recent_empty"        to ("You haven't used any effect yet." to "هنوز از هیچ افکتی استفاده نکرده‌اید."),

        // Custom
        "custom_title"        to ("Custom Voice" to "صدای سفارشی"),
        "custom_preview"      to ("Preview" to "پیش‌نمایش"),
        "custom_save"         to ("Save preset" to "ذخیره پریست"),
        "custom_saved"        to ("Preset saved" to "پریست ذخیره شد"),
        "custom_name"         to ("Preset name" to "نام پریست"),
        "custom_pitch"        to ("Pitch" to "زیر و بمی"),
        "custom_formant"      to ("Formant" to "فرمانت"),
        "custom_speed"        to ("Speed" to "سرعت"),
        "custom_bass"         to ("Bass" to "باس"),
        "custom_treble"       to ("Treble" to "زیر"),
        "custom_reverb"       to ("Reverb" to "ریورب"),
        "custom_echo"         to ("Echo" to "اکو"),
        "custom_delay"        to ("Delay" to "دیله"),
        "custom_distortion"   to ("Distortion" to "دیستورشن"),
        "custom_chorus"       to ("Chorus" to "کر"),
        "custom_flanger"      to ("Flanger" to "فلنجر"),
        "custom_phaser"       to ("Phaser" to "فیزر"),
        "custom_volume"       to ("Volume" to "بلندی"),
        "custom_need_recording" to ("Please record something first to preview a custom voice." to "برای پیش‌نمایش صدای سفارشی، ابتدا چیزی ضبط کنید."),

        // Settings
        "set_title"           to ("Settings" to "تنظیمات"),
        "set_language"        to ("Language" to "زبان"),
        "set_language_fa"     to ("فارسی" to "فارسی"),
        "set_language_en"     to ("English" to "English"),
        "set_appearance"      to ("Appearance" to "ظاهر"),
        "set_light"           to ("Light" to "روشن"),
        "set_dark"            to ("Dark" to "تاریک"),
        "set_system"          to ("System" to "سیستم"),
        "set_audio"           to ("Audio" to "صدا"),
        "set_format"          to ("Recording format" to "قالب ضبط"),
        "set_quality"         to ("Recording quality" to "کیفیت ضبط"),
        "set_sr"              to ("Sample rate" to "نرخ نمونه‌برداری"),
        "set_output"          to ("Output format" to "قالب خروجی"),
        "set_processing"      to ("Processing" to "پردازش"),
        "set_proc_quality"    to ("Processing quality" to "کیفیت پردازش"),
        "set_keep_orig"       to ("Keep original recordings" to "نگه‌داشتن ضبط‌های اصلی"),
        "set_auto_save"       to ("Automatically save processed files" to "ذخیره خودکار فایل‌های پردازش‌شده"),
        "set_library"         to ("Library" to "کتابخانه"),
        "set_sort"            to ("Default sorting" to "چیدمان پیش‌فرض"),
        "set_sort_date"       to ("Date" to "تاریخ"),
        "set_sort_name"       to ("Name" to "نام"),
        "set_sort_duration"   to ("Duration" to "مدت"),
        "set_show_fav"        to ("Show favorites section" to "نمایش بخش علاقه‌مندی‌ها"),
        "set_show_recent"     to ("Show recent effects" to "نمایش افکت‌های اخیر"),
        "set_about"           to ("About" to "درباره"),
        "set_version"         to ("App version" to "نسخه برنامه"),

        // Qualities
        "q_low"               to ("Low" to "پایین"),
        "q_medium"            to ("Medium" to "متوسط"),
        "q_high"              to ("High" to "بالا"),
        "q_fast"              to ("Fast" to "سریع"),
        "q_balanced"          to ("Balanced" to "متعادل"),

        // Permissions & errors
        "perm_mic_needed"     to ("Microphone permission is needed to record." to "برای ضبط، اجازه دسترسی به میکروفن نیاز است."),
        "perm_mic_denied"     to ("Microphone permission denied." to "اجازه دسترسی به میکروفن رد شد."),
        "err_rec_failed"      to ("Recording failed. Please try again." to "ضبط با شکست مواجه شد. دوباره امتحان کنید."),
        "err_unsupported"     to ("Unsupported audio file." to "فایل صوتی پشتیبانی نمی‌شود."),
        "err_process"         to ("Voice processing failed. Please try again." to "پردازش صدا با شکست مواجه شد. دوباره امتحان کنید."),
        "err_storage"         to ("Not enough storage available." to "فضای ذخیره‌سازی کافی نیست."),
        "err_corrupt"         to ("The recording appears to be corrupted." to "فایل ضبط شده خراب است."),
        "err_play"            to ("Playback failed." to "پخش با شکست مواجه شد."),
        "err_silent"          to ("The recording was too quiet. Please try again." to "ضبط بسیار کم‌صدا بود. دوباره امتحان کنید."),
        "err_too_long"        to ("This audio is too long to process (max about 6 minutes)." to "این صوت برای پردازش خیلی طولانی است (حداکثر حدود ۶ دقیقه)."),
        "err_import"          to ("Could not import this audio file." to "وارد کردن این فایل صوتی ممکن نبود."),
        "err_share"           to ("Could not prepare the file for sharing." to "آماده‌سازی فایل برای اشتراک‌گذاری ممکن نبود."),
        "home_import"         to ("Import Audio" to "وارد کردن صوت"),
        "rec_limit"           to ("Maximum length reached. Tap stop to save." to "به حداکثر طول ضبط رسیدید. برای ذخیره، پایان را لمس کنید."),
        "rec_recording"       to ("Recording…" to "در حال ضبط…"),
        "saved"               to ("Saved to library" to "در کتابخانه ذخیره شد"),
        "err_memory"          to ("The device ran low on memory during processing." to "حافظه دستگاه در حین پردازش کم شد."),

        // Splash
        "splash_line1"        to ("VoiceX" to "VoiceX"),
        "splash_line2"        to ("Made by CODEx" to "ساخته توسط CODEx"),

        // Misc
        "yes"                 to ("Yes" to "بله"),
        "no"                  to ("No" to "خیر"),
        "back"                to ("Back" to "بازگشت"),
        "close"               to ("Close" to "بستن"),
        "duration_short"      to ("s" to "ث"),
        "empty_state"         to ("Nothing to show yet." to "چیزی برای نمایش نیست.")
    )

    fun get(key: String, language: String): String {
        val pair = map[key] ?: return key
        return if (language == "fa") pair.second else pair.first
    }

    fun quality(key: String, language: String): String = get(key, language)
}

val LocalLanguage = compositionLocalOf { "fa" }

@Composable
fun s(key: String): String = Strings.get(key, LocalLanguage.current)
