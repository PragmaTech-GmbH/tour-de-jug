(function () {
    var map = L.map('map', {
        zoomControl: false
    }).setView([45, 10], 4);

    L.control.zoom({ position: 'topright' }).addTo(map);

    L.tileLayer('https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> &copy; <a href="https://carto.com/">CARTO</a>',
        maxZoom: 19,
        subdomains: 'abcd'
    }).addTo(map);

    var jugIcon = L.divIcon({
        html: '<div style="background:#ea580c;width:12px;height:12px;border-radius:50%;border:2px solid #fff;box-shadow:0 2px 6px rgba(0,0,0,0.25);"></div>',
        className: '',
        iconSize: [12, 12],
        iconAnchor: [6, 6],
        popupAnchor: [0, -8]
    });

    var markers = {};
    var jugListEl = document.getElementById('jugList');
    var searchInput = document.getElementById('searchInput');

    function escHtml(str) {
        return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }

    function renderList(data) {
        jugListEl.innerHTML = '';
        data.forEach(function (jug) {
            var item = document.createElement('div');
            item.className = 'jug-item px-4 py-3 cursor-pointer';
            item.dataset.slug = jug.slug;
            var nameHtml = '<p class="font-medium text-sm leading-snug ' + (jug.inactive ? 'text-gray-400' : 'text-gray-800') + '">' + escHtml(jug.name) +
                (jug.inactive ? ' <span class="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-medium bg-gray-100 text-gray-500">Inactive</span>' : '') + '</p>';
            item.innerHTML = nameHtml +
                '<p class="text-xs text-gray-400 mt-0.5">' + escHtml(jug.city) + ', ' + escHtml(jug.country) + '</p>';
            item.addEventListener('click', function () {
                selectJug(jug.slug);
            });
            jugListEl.appendChild(item);
        });
    }

    function selectJug(slug) {
        document.querySelectorAll('.jug-item').forEach(function (el) {
            el.classList.remove('active');
        });
        var listItem = document.querySelector('.jug-item[data-slug="' + slug + '"]');
        if (listItem) {
            listItem.classList.add('active');
            listItem.scrollIntoView({ block: 'nearest' });
        }
        if (markers[slug]) {
            markers[slug].openPopup();
            map.flyTo(markers[slug].getLatLng(), 8, { duration: 0.8 });
        }
    }

    jugsData.forEach(function (jug) {
        if (jug.lat != null && jug.lng != null) {
            var marker = L.marker([jug.lat, jug.lng], { icon: jugIcon }).addTo(map);
            marker.bindPopup(
                '<div style="min-width:160px">' +
                '<strong style="font-size:14px;"><a href="/jugs/' + jug.slug + '" style="color:#c2410c;text-decoration:none;">' + escHtml(jug.name) + '</a></strong>' +
                '<br><span style="color:#71717a;font-size:12px;">' + escHtml(jug.city) + ', ' + escHtml(jug.country) + '</span>' +
                (jug.homepageUrl ? '<br><a href="' + jug.homepageUrl + '" target="_blank" style="color:#ea580c;font-size:12px;text-decoration:none;">Visit website &rarr;</a>' : '') +
                '</div>'
            );
            marker.on('click', function () {
                selectJug(jug.slug);
            });
            markers[jug.slug] = marker;
        }
    });

    renderList(jugsData);

    searchInput.addEventListener('input', function () {
        var query = this.value.toLowerCase().trim();
        var filtered = jugsData.filter(function (j) {
            return j.name.toLowerCase().includes(query) ||
                j.city.toLowerCase().includes(query) ||
                j.country.toLowerCase().includes(query);
        });
        renderList(filtered);
    });
})();
