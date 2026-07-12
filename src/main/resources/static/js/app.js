/**
 * MTD Submitter — Client-side JavaScript
 *
 * Handles:
 * - Fraud prevention header data collection (required by HMRC)
 * - UI enhancements
 */

// ==========================================
// Fraud Prevention Data Collection
// HMRC requires these for WEB_APP_VIA_SERVER
// ==========================================
const FraudPrevention = {

    /**
     * Collect all browser-side fraud prevention data.
     * This is sent to the server with each HMRC API request.
     */
    collectData: function () {
        const data = {};

        // Device ID (persistent UUID in localStorage)
        if (!localStorage.getItem('mtd_device_id')) {
            localStorage.setItem('mtd_device_id', crypto.randomUUID());
        }
        data.deviceId = localStorage.getItem('mtd_device_id');

        // Timezone
        data.timezone = Intl.DateTimeFormat().resolvedOptions().timeZone;
        const offset = new Date().getTimezoneOffset();
        const sign = offset <= 0 ? '+' : '-';
        const hours = String(Math.floor(Math.abs(offset) / 60)).padStart(2, '0');
        const mins = String(Math.abs(offset) % 60).padStart(2, '0');
        data.timezoneOffset = `UTC${sign}${hours}:${mins}`;

        // Screen info
        data.screenWidth = screen.width;
        data.screenHeight = screen.height;
        data.screenColourDepth = screen.colorDepth;
        data.scalingFactor = window.devicePixelRatio || 1;

        // Window size
        data.windowWidth = window.innerWidth;
        data.windowHeight = window.innerHeight;

        // Browser info
        data.userAgent = navigator.userAgent;
        data.doNotTrack = navigator.doNotTrack === '1' || navigator.doNotTrack === 'yes';

        // Plugins
        const plugins = [];
        if (navigator.plugins) {
            for (let i = 0; i < Math.min(navigator.plugins.length, 10); i++) {
                plugins.push(navigator.plugins[i].name);
            }
        }
        data.plugins = plugins.join(',');

        return data;
    },

    /**
     * Store fraud prevention data in a hidden form field or session.
     */
    storeInSession: function () {
        const data = this.collectData();
        // Store as a cookie that the server can read
        document.cookie = `mtd_fp_data=${encodeURIComponent(JSON.stringify(data))}; path=/; SameSite=Lax`;
    }
};

// Collect fraud prevention data on page load
document.addEventListener('DOMContentLoaded', function () {
    FraudPrevention.storeInSession();
});
