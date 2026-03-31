(function () {
    var speakerSearchInput = document.getElementById('speakerSearchInput');
    var speakerListEl = document.getElementById('speakerList');
    var topSpeakerItems = speakerListEl.querySelectorAll('.top-speaker-item');

    function escHtml(str) {
        return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }

    function renderSpeakerResults(speakers) {
        // Hide top speakers
        topSpeakerItems.forEach(function (el) { el.style.display = 'none'; });

        // Remove previous search results
        speakerListEl.querySelectorAll('.speaker-search-result').forEach(function (el) { el.remove(); });

        speakers.forEach(function (speaker) {
            var item = document.createElement('div');
            item.className = 'speaker-search-result';
            var initial = speaker.displayName ? speaker.displayName.charAt(0) : '?';
            var avatarHtml = speaker.avatarUrl
                ? '<img src="' + escHtml(speaker.avatarUrl) + '" alt="" class="w-9 h-9 rounded-full"/>'
                : '<div class="w-9 h-9 rounded-full bg-brand-100 flex items-center justify-center text-brand-700 text-sm font-bold">' + escHtml(initial) + '</div>';

            item.innerHTML =
                '<a href="/profiles/' + escHtml(speaker.username) + '" class="flex items-center gap-3 py-2.5 px-2 rounded-lg hover:bg-gray-50 transition-colors group">' +
                '<div class="shrink-0">' + avatarHtml + '</div>' +
                '<div class="flex-1 min-w-0">' +
                '<p class="text-sm font-medium text-gray-800 truncate group-hover:text-brand-700 transition-colors">' + escHtml(speaker.displayName) + '</p>' +
                '<p class="text-xs text-gray-400">@' + escHtml(speaker.username) + '</p>' +
                '</div></a>';
            speakerListEl.appendChild(item);
        });

        if (speakers.length === 0) {
            var empty = document.createElement('div');
            empty.className = 'speaker-search-result text-xs text-gray-400 py-4 text-center';
            empty.textContent = 'No speakers found.';
            speakerListEl.appendChild(empty);
        }
    }

    function showTopSpeakers() {
        speakerListEl.querySelectorAll('.speaker-search-result').forEach(function (el) { el.remove(); });
        topSpeakerItems.forEach(function (el) { el.style.display = ''; });
    }

    speakerSearchInput.addEventListener('input', function () {
        var query = this.value.toLowerCase().trim();
        if (!query) {
            showTopSpeakers();
            return;
        }
        var filtered = speakersData.filter(function (s) {
            return s.displayName.toLowerCase().includes(query) ||
                s.username.toLowerCase().includes(query);
        });
        renderSpeakerResults(filtered);
    });
})();
