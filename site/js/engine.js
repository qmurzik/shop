/* «Сердце Двух Лун» — игровой движок: состояние, переходы, сохранения. */
(function () {
  "use strict";

  var DATA = window.GAME_DATA;
  var SAVE_KEY_PREFIX = "htm_save_";
  var AUTOSAVE_SLOT = "auto";
  var SETTINGS_KEY = "htm_settings";
  var STAT_CAP = 10;
  var AFFECTION_CAP = 12;

  function clamp(n, min, max) { return Math.max(min, Math.min(max, n)); }

  function freshState() {
    return {
      version: 1,
      playerName: "",
      originId: "",
      stats: { charm: 0, courage: 0, wisdom: 0 },
      affection: { elias: 0, lyra: 0, cassia: 0 },
      flags: {},
      inventory: [],
      memories: [],
      currentScene: DATA.startScene,
      startedAt: Date.now(),
      updatedAt: Date.now()
    };
  }

  function loadSettings() {
    var defaults = { textSpeed: 2, reduceMotion: false };
    try {
      var raw = localStorage.getItem(SETTINGS_KEY);
      if (!raw) return defaults;
      var parsed = JSON.parse(raw);
      return Object.assign(defaults, parsed);
    } catch (e) {
      return defaults;
    }
  }

  function saveSettings(settings) {
    try { localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings)); } catch (e) { /* storage unavailable */ }
  }

  var Engine = {
    state: null,
    settings: loadSettings(),
    listeners: [],

    on: function (fn) { this.listeners.push(fn); },
    emit: function (event, payload) {
      this.listeners.forEach(function (fn) { fn(event, payload); });
    },

    newGame: function (name, originId) {
      var s = freshState();
      s.playerName = (name || "").trim() || "Странница";
      s.originId = originId || DATA.origins[0].id;
      var origin = DATA.origins.filter(function (o) { return o.id === originId; })[0];
      if (origin) s.stats[origin.stat] = clamp(s.stats[origin.stat] + 1, 0, STAT_CAP);
      this.state = s;
      this.goTo(s.currentScene, true);
    },

    hasAnySave: function () {
      return !!this.readSlot(AUTOSAVE_SLOT) || [1, 2, 3].some(function (i) { return !!Engine.readSlot(i); });
    },

    slotKey: function (slot) { return SAVE_KEY_PREFIX + slot; },

    readSlot: function (slot) {
      try {
        var raw = localStorage.getItem(this.slotKey(slot));
        return raw ? JSON.parse(raw) : null;
      } catch (e) { return null; }
    },

    writeSlot: function (slot, state) {
      try {
        localStorage.setItem(this.slotKey(slot), JSON.stringify(state));
        return true;
      } catch (e) { return false; }
    },

    deleteSlot: function (slot) {
      try { localStorage.removeItem(this.slotKey(slot)); } catch (e) { /* ignore */ }
    },

    save: function (slot) {
      if (!this.state) return false;
      this.state.updatedAt = Date.now();
      return this.writeSlot(slot, this.state);
    },

    autosave: function () { this.save(AUTOSAVE_SLOT); },

    loadSlot: function (slot) {
      var data = this.readSlot(slot);
      if (!data) return false;
      this.state = data;
      this.render();
      return true;
    },

    listSlots: function () {
      var self = this;
      return [1, 2, 3].map(function (i) {
        var data = self.readSlot(i);
        return { slot: i, data: data };
      });
    },

    currentScene: function () {
      return DATA.scenes[this.state.currentScene];
    },

    currentChapterMeta: function () {
      var scene = this.currentScene();
      return DATA.chapters[scene.chapter] || { title: "" };
    },

    applyEffects: function (effects) {
      if (!effects) return;
      var s = this.state;
      if (effects.stats) {
        Object.keys(effects.stats).forEach(function (k) {
          s.stats[k] = clamp((s.stats[k] || 0) + effects.stats[k], 0, STAT_CAP);
        });
      }
      if (effects.affection) {
        Object.keys(effects.affection).forEach(function (k) {
          s.affection[k] = clamp((s.affection[k] || 0) + effects.affection[k], 0, AFFECTION_CAP);
        });
      }
      if (effects.flags) {
        Object.keys(effects.flags).forEach(function (k) { s.flags[k] = effects.flags[k]; });
      }
      if (effects.items) {
        effects.items.forEach(function (id) {
          if (s.inventory.indexOf(id) === -1) s.inventory.push(id);
        });
      }
    },

    resolveNext: function (next) {
      if (!next) return null;
      if (typeof next === "string") return next;
      if (typeof next === "object" && next.byFlag) {
        var val = this.state.flags[next.byFlag];
        return (val && next.map[val]) || next.fallback || null;
      }
      return null;
    },

    topAffectionCharacter: function () {
      var a = this.state.affection;
      var order = ["elias", "lyra", "cassia"];
      return order.reduce(function (best, id) { return a[id] > a[best] ? id : best; }, order[0]);
    },

    goTo: function (sceneId, isInitial) {
      var scene = DATA.scenes[sceneId];
      if (!scene) return;
      this.state.currentScene = sceneId;
      this.applyEffects(scene.effects);
      if (scene.unlock && this.state.memories.indexOf(scene.unlock) === -1) {
        this.state.memories.push(scene.unlock);
        this.emit("memory-unlocked", scene.unlock);
      }
      this.autosave();
      this.emit(isInitial ? "game-start" : "scene-enter", scene);
    },

    choose: function (choice) {
      this.applyEffects(choice.effects);
      var nextId = this.resolveNext(choice.next);
      if (nextId) this.goTo(nextId);
    },

    giveGift: function (itemId) {
      var companionId = this.state.flags.companion;
      var character = DATA.characters[companionId];
      var result = { companion: character, itemId: itemId };
      if (itemId) {
        var item = DATA.items[itemId];
        var gain = 1;
        if (item.tag === "sentimental") gain = 2;
        if (character && item.tag === character.tagPreference) gain = 3;
        this.applyEffects({ affection: (function () { var o = {}; o[companionId] = gain; return o; })() });
        this.state.inventory = this.state.inventory.filter(function (id) { return id !== itemId; });
        result.gain = gain;
        result.matched = character && item.tag === character.tagPreference;
        result.item = item;
      }
      var scene = this.currentScene();
      var nextId = this.resolveNext(scene.next);
      if (nextId) this.goTo(nextId);
      return result;
    },

    render: function () {
      this.emit("scene-enter", this.currentScene());
    },

    updateSetting: function (key, value) {
      this.settings[key] = value;
      saveSettings(this.settings);
      this.emit("settings-changed", this.settings);
    },

    resetAll: function () {
      [1, 2, 3, AUTOSAVE_SLOT].forEach(function (s) { Engine.deleteSlot(s); });
      this.state = null;
    }
  };

  window.Engine = Engine;
  window.GAME_CONST = { AUTOSAVE_SLOT: AUTOSAVE_SLOT, STAT_CAP: STAT_CAP, AFFECTION_CAP: AFFECTION_CAP };
})();
