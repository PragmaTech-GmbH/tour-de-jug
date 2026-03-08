(function () {
    const map = L.map('map').setView([20, 0], 2);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
        maxZoom: 19
    }).addTo(map);

    const markers = {};
    const jugListEl = document.getElementById('jugList');
    const searchInput = document.getElementById('searchInput');

    function renderList(data) {
        jugListEl.innerHTML = '';
        data.forEach(function (jug) {
            const item = document.createElement('div');
            item.className = 'jug-item px-4 py-3 cursor-pointer border-b border-gray-50 hover:bg-indigo-50 transition-colors';
            item.dataset.slug = jug.slug;
            item.innerHTML = '<p class="font-medium text-sm text-gray-800">' + escHtml(jug.name) + '</p>' +
                '<p class="text-xs text-gray-500">' + escHtml(jug.city) + ', ' + escHtml(jug.country) + '</p>';
            item.addEventListener('click', function () {
                selectJug(jug.slug);
            });
            jugListEl.appendChild(item);
        });
    }

    function escHtml(str) {
        return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }

    function selectJug(slug) {
        document.querySelectorAll('.jug-item').forEach(function (el) {
            el.classList.remove('active');
        });
        const listItem = document.querySelector('.jug-item[data-slug="' + slug + '"]');
        if (listItem) {
            listItem.classList.add('active');
            listItem.scrollIntoView({ block: 'nearest' });
        }
        if (markers[slug]) {
            markers[slug].openPopup();
            map.panTo(markers[slug].getLatLng());
        }
    }

    jugsData.forEach(function (jug) {
        const marker = L.marker([jug.lat, jug.lng]).addTo(map);
        marker.bindPopup(
            '<strong><a href="/jugs/' + jug.slug + '">' + escHtml(jug.name) + '</a></strong>' +
            '<br>' + escHtml(jug.city) + ', ' + escHtml(jug.country) +
            (jug.homepageUrl ? '<br><a href="' + jug.homepageUrl + '" target="_blank" class="text-blue-600">Website</a>' : '')
        );
        marker.on('click', function () {
            selectJug(jug.slug);
        });
        markers[jug.slug] = marker;
    });

    renderList(jugsData);

    searchInput.addEventListener('input', function () {
        const query = this.value.toLowerCase().trim();
        const filtered = jugsData.filter(function (j) {
            return j.name.toLowerCase().includes(query) ||
                j.city.toLowerCase().includes(query) ||
                j.country.toLowerCase().includes(query);
        });
        renderList(filtered);
    });
})();
