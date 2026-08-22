/* «Сердце Двух Лун» — интерфейс: экраны, модальные окна, эффекты. */
(function () {
  "use strict";

  var DATA = window.GAME_DATA;
  var Engine = window.Engine;

  var el = {};
  function q(id) { return document.getElementById(id); }

  function cacheEls() {
    ["screen-start", "screen-newgame", "screen-game", "screen-ending",
      "btn-continue", "btn-new-game", "btn-load", "btn-gallery", "btn-settings", "btn-about",
      "input-name", "origin-choices", "btn-back-start", "btn-start-story",
      "btn-menu", "chapter-title", "hearts-row", "scene-stage", "portraits",
      "dialogue-box", "speaker-name", "dialogue-text", "choices", "btn-continue-scene",
      "ending-emblem", "ending-title", "ending-text", "ending-stats",
      "btn-ending-gallery", "btn-ending-newgame", "btn-ending-menu",
      "modal-backdrop", "modal-title", "modal-body", "modal-close", "modal-back",
      "toast", "starfield"
    ].forEach(function (id) { el[id.replace(/-([a-z])/g, function (_, c) { return c.toUpperCase(); })] = q(id); });
  }

  function showScreen(id) {
    document.querySelectorAll(".screen").forEach(function (s) { s.classList.remove("active"); });
    q(id).classList.add("active");
  }

  var toastTimer = null;
  function toast(msg) {
    el.toast.textContent = msg;
    el.toast.classList.add("show");
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { el.toast.classList.remove("show"); }, 2200);
  }

  /* ---------------- Starfield ---------------- */
  var Starfield = (function () {
    var canvas, ctx, stars = [], raf;
    function init() {
      canvas = el.starfield;
      ctx = canvas.getContext("2d");
      resize();
      window.addEventListener("resize", resize);
      loop();
    }
    function resize() {
      var dpr = Math.min(window.devicePixelRatio || 1, 2);
      canvas.width = canvas.clientWidth * dpr;
      canvas.height = canvas.clientHeight * dpr;
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      var count = Math.floor((canvas.clientWidth * canvas.clientHeight) / 9000);
      stars = [];
      for (var i = 0; i < count; i++) {
        stars.push({
          x: Math.random() * canvas.clientWidth,
          y: Math.random() * canvas.clientHeight,
          r: Math.random() * 1.3 + 0.2,
          phase: Math.random() * Math.PI * 2,
          speed: 0.4 + Math.random() * 0.8
        });
      }
    }
    function loop(t) {
      if (!Engine.settings.reduceMotion) {
        ctx.clearRect(0, 0, canvas.clientWidth, canvas.clientHeight);
        var time = (t || 0) / 1000;
        for (var i = 0; i < stars.length; i++) {
          var s = stars[i];
          var a = 0.35 + 0.65 * Math.abs(Math.sin(time * s.speed + s.phase));
          ctx.beginPath();
          ctx.fillStyle = "rgba(255,255,255," + a.toFixed(2) + ")";
          ctx.arc(s.x, s.y, s.r, 0, Math.PI * 2);
          ctx.fill();
        }
      }
      raf = requestAnimationFrame(loop);
    }
    return { init: init };
  })();

  /* ---------------- Typewriter ---------------- */
  var typing = null;
  function typeText(container, text, speedLevel, onDone) {
    if (typing) clearInterval(typing);
    container.textContent = "";
    var full = text || "";
    if (speedLevel === 0) {
      container.textContent = full;
      if (onDone) onDone();
      return;
    }
    var speeds = { 1: 55, 2: 24, 3: 8 };
    var interval = speeds[speedLevel] || 24;
    var i = 0;
    typing = setInterval(function () {
      i++;
      container.textContent = full.slice(0, i);
      if (i >= full.length) {
        clearInterval(typing);
        typing = null;
        if (onDone) onDone();
      }
    }, interval);
    container._skip = function () {
      clearInterval(typing);
      typing = null;
      container.textContent = full;
      if (onDone) onDone();
    };
  }

  /* ---------------- Game screen rendering ---------------- */
  function renderHearts() {
    var st = Engine.state;
    el.heartsRow.innerHTML = "";
    ["elias", "lyra", "cassia"].forEach(function (id) {
      if (!st.flags["met_" + id]) return;
      var chip = document.createElement("div");
      chip.className = "heart-chip";
      chip.title = DATA.characters[id].name + ": " + st.affection[id];
      chip.textContent = DATA.characters[id].icon;
      el.heartsRow.appendChild(chip);
    });
  }

  function renderPortraits(ids) {
    el.portraits.innerHTML = "";
    (ids || []).forEach(function (id) {
      var wrap = document.createElement("div");
      wrap.className = "portrait";
      wrap.dataset.char = id;
      var fig = document.createElement("div");
      fig.className = "portrait-figure";
      wrap.appendChild(fig);
      var badge = document.createElement("div");
      badge.className = "portrait-badge";
      var ch = DATA.characters[id];
      badge.textContent = ch ? ch.icon + " " + ch.name.split(" ")[0] : "";
      wrap.appendChild(badge);
      el.portraits.appendChild(wrap);
    });
  }

  function renderChoices(scene) {
    el.choices.innerHTML = "";
    el.btnContinueScene.hidden = true;

    if (scene.type === "gift") {
      var st = Engine.state;
      if (st.inventory.length === 0) {
        var skipBtn = document.createElement("button");
        skipBtn.className = "btn btn-continue";
        skipBtn.textContent = "У вас нет подарков — идти дальше ▸";
        skipBtn.onclick = function () { Engine.giveGift(null); };
        el.choices.appendChild(skipBtn);
        return;
      }
      st.inventory.forEach(function (itemId) {
        var item = DATA.items[itemId];
        var btn = document.createElement("button");
        btn.className = "choice-btn";
        btn.innerHTML = "Подарить: " + item.icon + " " + item.name;
        btn.onclick = function () {
          var res = Engine.giveGift(itemId);
          toast(res.matched ? "Подарок пришёлся точно по сердцу (+" + res.gain + ")" : "Подарок принят тепло (+" + res.gain + ")");
        };
        el.choices.appendChild(btn);
      });
      var noneBtn = document.createElement("button");
      noneBtn.className = "choice-btn";
      noneBtn.textContent = "Не дарить ничего сейчас";
      noneBtn.onclick = function () { Engine.giveGift(null); };
      el.choices.appendChild(noneBtn);
      return;
    }

    if (scene.choices && scene.choices.length) {
      scene.choices.forEach(function (choice) {
        var btn = document.createElement("button");
        btn.className = "choice-btn";
        btn.textContent = choice.label + (choice.tag ? "" : "");
        if (choice.tag) {
          var tag = document.createElement("span");
          tag.className = "tag";
          tag.textContent = "· " + choice.tag;
          btn.appendChild(tag);
        }
        btn.onclick = function () { Engine.choose(choice); };
        el.choices.appendChild(btn);
      });
      return;
    }

    if (scene.next) {
      el.btnContinueScene.hidden = false;
      el.btnContinueScene.onclick = function () {
        var nextId = Engine.resolveNext(scene.next);
        if (nextId) Engine.goTo(nextId);
      };
    }
  }

  function renderScene(scene) {
    if (scene.isEnding) { renderEnding(scene); return; }
    showScreen("screen-game");
    el.chapterTitle.textContent = Engine.currentChapterMeta().title;
    el.sceneStage.querySelector(".scene-glow").style.background = (DATA.chapters[scene.chapter] || {}).glow || "";
    renderPortraits(scene.portraits);
    el.speakerName.textContent = scene.speaker && scene.speaker !== "narrator" ? scene.speaker : "";
    renderHearts();
    el.choices.innerHTML = "";
    el.btnContinueScene.hidden = true;
    el.dialogueBox.scrollTop = 0;
    typeText(el.dialogueText, scene.text, Engine.settings.textSpeed, function () {
      renderChoices(scene);
    });
    el.dialogueBox.onclick = function (evt) {
      if (evt.target.closest(".choice-btn, .btn")) return;
      if (el.dialogueText._skip) el.dialogueText._skip();
    };
  }

  function renderEnding(scene) {
    showScreen("screen-ending");
    el.endingEmblem.textContent = scene.endingEmblem || "🌙";
    el.endingTitle.textContent = scene.endingTitle || "Конец";
    el.endingText.textContent = scene.text;
    var st = Engine.state;
    var lines = [
      "Имя: " + st.playerName,
      "Обаяние " + st.stats.charm + " · Смелость " + st.stats.courage + " · Мудрость " + st.stats.wisdom,
      "Привязанность — Элиас: " + st.affection.elias + ", Лира: " + st.affection.lyra + ", Кассия: " + st.affection.cassia,
      "Воспоминаний собрано: " + st.memories.length + " из " + DATA.memories.length
    ];
    el.endingStats.innerHTML = "";
    lines.forEach(function (line) {
      var d = document.createElement("div");
      d.textContent = line;
      el.endingStats.appendChild(d);
    });
    Engine.deleteSlot(window.GAME_CONST.AUTOSAVE_SLOT);
  }

  /* ---------------- New game screen ---------------- */
  var selectedOrigin = null;
  function renderOrigins() {
    el.originChoices.innerHTML = "";
    DATA.origins.forEach(function (origin) {
      var card = document.createElement("div");
      card.className = "origin-card";
      card.setAttribute("role", "radio");
      card.setAttribute("tabindex", "0");
      card.innerHTML = "<strong>" + origin.title + "</strong><span>" + origin.desc + "</span>";
      card.onclick = function () {
        selectedOrigin = origin.id;
        document.querySelectorAll(".origin-card").forEach(function (c) { c.classList.remove("selected"); });
        card.classList.add("selected");
      };
      el.originChoices.appendChild(card);
    });
    selectedOrigin = DATA.origins[0].id;
    el.originChoices.firstChild.classList.add("selected");
  }

  /* ---------------- Modal system ---------------- */
  var Modal = {
    open: function (type, opts) {
      el.modalBackdrop.hidden = false;
      el.modalBack.hidden = !(opts && opts.showBack);
      el.modalBack.onclick = function () { Modal.open("menu"); };
      this.render(type);
    },
    close: function () { el.modalBackdrop.hidden = true; },
    render: function (type) {
      var titleMap = {
        menu: "Меню", characters: "Персонажи", inventory: "Инвентарь",
        saves: "Сохранения", settings: "Настройки", gallery: "Галерея воспоминаний", about: "Об игре"
      };
      el.modalTitle.textContent = titleMap[type] || "Меню";
      el.modalBody.innerHTML = "";
      var renderer = renderers[type];
      if (renderer) renderer(el.modalBody);
    }
  };

  var renderers = {
    menu: function (body) {
      var list = document.createElement("div");
      list.className = "menu-list";
      var items = [
        ["Персонажи", "characters"],
        ["Инвентарь", "inventory"],
        ["Сохранения", "saves"],
        ["Настройки", "settings"],
        ["Галерея воспоминаний", "gallery"]
      ];
      items.forEach(function (pair) {
        var btn = document.createElement("button");
        btn.className = "btn btn-block";
        btn.textContent = pair[0];
        btn.onclick = function () { Modal.open(pair[1], { showBack: true }); };
        list.appendChild(btn);
      });
      var quitBtn = document.createElement("button");
      quitBtn.className = "btn btn-block btn-danger";
      quitBtn.textContent = "Выйти в главное меню";
      quitBtn.onclick = function () {
        Engine.autosave();
        Modal.close();
        showScreen("screen-start");
        window.App.refreshStartScreen();
      };
      list.appendChild(quitBtn);
      body.appendChild(list);
    },

    characters: function (body) {
      var st = Engine.state;
      var any = false;
      ["elias", "lyra", "cassia"].forEach(function (id) {
        if (!st.flags["met_" + id]) return;
        any = true;
        var ch = DATA.characters[id];
        var pct = Math.round((st.affection[id] / window.GAME_CONST.AFFECTION_CAP) * 100);
        var card = document.createElement("div");
        card.className = "char-card";
        card.innerHTML =
          '<div class="char-card-head"><div class="char-dot" style="background:' + ch.color + '"></div>' +
          "<div><h3>" + ch.icon + " " + ch.name + "</h3><div class=\"char-title\">" + ch.title + "</div></div></div>" +
          '<p class="bio">' + ch.bio + "</p>" +
          '<div class="stat-row"><span class="stat-label">Привязанность</span><div class="stat-bar"><i style="width:' + pct + '%;background:' + ch.color + '"></i></div><span class="stat-value">' + st.affection[id] + "</span></div>";
        body.appendChild(card);
      });
      var statBlock = document.createElement("div");
      statBlock.className = "char-card";
      statBlock.innerHTML = "<h3>" + st.playerName + "</h3>";
      [["Обаяние", st.stats.charm], ["Смелость", st.stats.courage], ["Мудрость", st.stats.wisdom]].forEach(function (pair) {
        var row = document.createElement("div");
        row.className = "stat-row";
        var pct = Math.round((pair[1] / window.GAME_CONST.STAT_CAP) * 100);
        row.innerHTML = '<span class="stat-label">' + pair[0] + '</span><div class="stat-bar"><i style="width:' + pct + '%"></i></div><span class="stat-value">' + pair[1] + "</span>";
        statBlock.appendChild(row);
      });
      body.appendChild(statBlock);
      if (!any) {
        var note = document.createElement("p");
        note.className = "empty-note";
        note.textContent = "Вы ещё ни с кем не познакомились.";
        body.insertBefore(note, body.firstChild);
      }
    },

    inventory: function (body) {
      var st = Engine.state;
      if (st.inventory.length === 0) {
        var note = document.createElement("p");
        note.className = "empty-note";
        note.textContent = "Пока пусто. Предметы появятся по ходу истории.";
        body.appendChild(note);
        return;
      }
      var grid = document.createElement("div");
      grid.className = "item-grid";
      st.inventory.forEach(function (id) {
        var item = DATA.items[id];
        var card = document.createElement("div");
        card.className = "item-card";
        card.title = item.desc;
        card.innerHTML = '<div class="item-icon">' + item.icon + '</div><div class="item-name">' + item.name + "</div>";
        grid.appendChild(card);
      });
      body.appendChild(grid);
    },

    saves: function (body) {
      var wrap = document.createElement("div");
      var manualBtn = document.createElement("button");
      manualBtn.className = "btn btn-block btn-primary";
      manualBtn.style.marginBottom = "14px";
      manualBtn.textContent = "Быстрое сохранение в слот 1";
      manualBtn.onclick = function () {
        Engine.save(1);
        toast("Игра сохранена");
        Modal.render("saves");
      };
      if (Engine.state) wrap.appendChild(manualBtn);

      Engine.listSlots().forEach(function (entry) {
        var row = document.createElement("div");
        row.className = "save-slot";
        var info = document.createElement("div");
        info.className = "save-info";
        if (entry.data) {
          info.innerHTML = "<strong>Слот " + entry.slot + " — " + entry.data.playerName + "</strong>" +
            new Date(entry.data.updatedAt).toLocaleString("ru-RU");
        } else {
          info.innerHTML = "<strong>Слот " + entry.slot + "</strong>Пусто";
        }
        row.appendChild(info);
        var actions = document.createElement("div");
        actions.className = "save-actions";
        if (Engine.state) {
          var saveBtn = document.createElement("button");
          saveBtn.className = "btn";
          saveBtn.textContent = "Сохранить";
          saveBtn.onclick = function () { Engine.save(entry.slot); toast("Сохранено в слот " + entry.slot); Modal.render("saves"); };
          actions.appendChild(saveBtn);
        }
        if (entry.data) {
          var loadBtn = document.createElement("button");
          loadBtn.className = "btn btn-primary";
          loadBtn.textContent = "Загрузить";
          loadBtn.onclick = function () {
            Engine.loadSlot(entry.slot);
            Modal.close();
            showScreen("screen-game");
          };
          actions.appendChild(loadBtn);
        }
        row.appendChild(actions);
        wrap.appendChild(row);
      });
      body.appendChild(wrap);
    },

    settings: function (body) {
      var wrap = document.createElement("div");
      var speedRow = document.createElement("div");
      speedRow.className = "setting-row";
      speedRow.innerHTML = '<label>Скорость текста</label>';
      var range = document.createElement("input");
      range.type = "range"; range.min = "0"; range.max = "3"; range.step = "1";
      range.value = String(Engine.settings.textSpeed);
      var labelSpan = document.createElement("div");
      var speedNames = ["Мгновенно", "Медленно", "Обычно", "Быстро"];
      labelSpan.style.cssText = "font-size:12px;color:var(--text-dim);margin-top:4px;";
      labelSpan.textContent = speedNames[Engine.settings.textSpeed];
      range.oninput = function () {
        Engine.updateSetting("textSpeed", Number(range.value));
        labelSpan.textContent = speedNames[Number(range.value)];
      };
      speedRow.appendChild(range);
      speedRow.appendChild(labelSpan);
      wrap.appendChild(speedRow);

      var toggleRow = document.createElement("div");
      toggleRow.className = "toggle-row";
      toggleRow.innerHTML = "<span>Снизить анимацию (звёзды, переходы)</span>";
      var sw = document.createElement("label");
      sw.className = "switch";
      var input = document.createElement("input");
      input.type = "checkbox";
      input.checked = !!Engine.settings.reduceMotion;
      input.onchange = function () {
        Engine.updateSetting("reduceMotion", input.checked);
        document.body.classList.toggle("reduce-motion", input.checked);
      };
      var track = document.createElement("span");
      track.className = "track";
      sw.appendChild(input);
      sw.appendChild(track);
      toggleRow.appendChild(sw);
      wrap.appendChild(toggleRow);

      var resetRow = document.createElement("div");
      resetRow.style.marginTop = "22px";
      var resetBtn = document.createElement("button");
      resetBtn.className = "btn btn-block btn-danger";
      resetBtn.textContent = "Сбросить весь прогресс";
      resetBtn.onclick = function () {
        if (confirm("Удалить все сохранения безвозвратно?")) {
          Engine.resetAll();
          Modal.close();
          showScreen("screen-start");
          window.App.refreshStartScreen();
          toast("Прогресс сброшен");
        }
      };
      resetRow.appendChild(resetBtn);
      wrap.appendChild(resetRow);
      body.appendChild(wrap);
    },

    gallery: function (body) {
      DATA.memories.forEach(function (mem) {
        var unlocked = Engine.state && Engine.state.memories.indexOf(mem.id) !== -1;
        var card = document.createElement("div");
        card.className = "memory-card" + (unlocked ? "" : " locked");
        card.innerHTML = "<h3>" + (unlocked ? mem.title : "???") + "</h3><p>" + (unlocked ? mem.desc : "Ещё не открыто") + "</p>";
        body.appendChild(card);
      });
    },

    about: function (body) {
      var wrap = document.createElement("div");
      wrap.className = "about-text";
      wrap.innerHTML =
        "<p><strong>«Сердце Двух Лун»</strong> — романтическая веб-RPG о городе Лунхейвен, старой магии и трёх людях, чьи судьбы вы можете изменить.</p>" +
        "<p>Исследуйте сюжет, делайте выбор, стройте отношения с Элиасом, Лирой и Кассией, собирайте предметы и дарите их с умом — а затем узнайте один из шести финалов.</p>" +
        "<p>Игра полностью сохраняется в вашем браузере и не требует подключения к интернету после загрузки.</p>";
      body.appendChild(wrap);
    }
  };

  window.UI = {
    el: el, cacheEls: cacheEls, showScreen: showScreen, toast: toast,
    Starfield: Starfield, renderScene: renderScene, renderOrigins: renderOrigins,
    getSelectedOrigin: function () { return selectedOrigin; },
    Modal: Modal
  };
})();
