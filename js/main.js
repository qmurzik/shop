/* «Сердце Двух Лун» — точка входа: обработчики экрана и запуск игры. */
(function () {
  "use strict";

  var Engine = window.Engine;
  var UI = window.UI;
  var GAME_CONST = window.GAME_CONST;

  function refreshStartScreen() {
    var hasAuto = !!Engine.readSlot(GAME_CONST.AUTOSAVE_SLOT);
    UI.el.btnContinue.hidden = !hasAuto;
  }

  Engine.on(function (event, payload) {
    if (event === "scene-enter" || event === "game-start") {
      UI.renderScene(payload);
    }
    if (event === "memory-unlocked") {
      var mem = window.GAME_DATA.memories.filter(function (m) { return m.id === payload; })[0];
      if (mem) UI.toast("Новое воспоминание: " + mem.title);
    }
  });

  function bindStartScreen() {
    UI.el.btnNewGame.onclick = function () {
      UI.el.inputName.value = "";
      UI.renderOrigins();
      UI.showScreen("screen-newgame");
    };
    UI.el.btnContinue.onclick = function () {
      if (Engine.loadSlot(GAME_CONST.AUTOSAVE_SLOT)) UI.showScreen("screen-game");
    };
    UI.el.btnLoad.onclick = function () { UI.Modal.open("saves"); };
    UI.el.btnGallery.onclick = function () { UI.Modal.open("gallery"); };
    UI.el.btnSettings.onclick = function () { UI.Modal.open("settings"); };
    UI.el.btnAbout.onclick = function () { UI.Modal.open("about"); };
  }

  function bindNewGameScreen() {
    UI.el.btnBackStart.onclick = function () { UI.showScreen("screen-start"); };
    UI.el.btnStartStory.onclick = function () {
      var name = UI.el.inputName.value.trim();
      var origin = UI.getSelectedOrigin();
      Engine.newGame(name, origin);
    };
  }

  function bindGameScreen() {
    UI.el.btnMenu.onclick = function () { UI.Modal.open("menu"); };
  }

  function bindEndingScreen() {
    UI.el.btnEndingGallery.onclick = function () { UI.Modal.open("gallery"); };
    UI.el.btnEndingNewgame.onclick = function () {
      UI.el.inputName.value = "";
      UI.renderOrigins();
      UI.showScreen("screen-newgame");
    };
    UI.el.btnEndingMenu.onclick = function () {
      UI.showScreen("screen-start");
      refreshStartScreen();
    };
  }

  function bindModal() {
    UI.el.modalClose.onclick = function () { UI.Modal.close(); };
    UI.el.modalBackdrop.onclick = function (evt) {
      if (evt.target === UI.el.modalBackdrop) UI.Modal.close();
    };
    document.addEventListener("keydown", function (evt) {
      if (evt.key === "Escape" && !UI.el.modalBackdrop.hidden) UI.Modal.close();
    });
  }

  function init() {
    UI.cacheEls();
    if (Engine.settings.reduceMotion) document.body.classList.add("reduce-motion");
    UI.Starfield.init();
    bindStartScreen();
    bindNewGameScreen();
    bindGameScreen();
    bindEndingScreen();
    bindModal();
    refreshStartScreen();
    UI.showScreen("screen-start");
  }

  window.App = { refreshStartScreen: refreshStartScreen };

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
