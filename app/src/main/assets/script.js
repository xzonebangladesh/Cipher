// ====== সেটিংস ======
var PARENT_PIN = "1234"; // এখানে নিজের পিন বসান
var STORAGE_KEY = "cipher_lock_at";

// pinIntent: পিন সঠিক হওয়ার পর কী করতে হবে ("timer" বা "exit")
var pinIntent = null;

// ====== ভিউ সুইচিং ======
function showView(id) {
  document.querySelectorAll(".view").forEach(function (el) {
    el.classList.remove("view-active");
  });
  document.getElementById(id).classList.add("view-active");
}

// ====== Android bridge wrappers ======
function setBackDisabled() {
  if (window.AndroidApp && window.AndroidApp.setBackButtonEnabled) {
    window.AndroidApp.setBackButtonEnabled(false);
  }
}
function setBackEnabled() {
  if (window.AndroidApp && window.AndroidApp.setBackButtonEnabled) {
    window.AndroidApp.setBackButtonEnabled(true);
  }
}
function exitApp() {
  if (window.AndroidApp && window.AndroidApp.exitApp) {
    window.AndroidApp.exitApp();
  }
}

// ====== ইউটিউব ======
function loadYoutube() {
  document.getElementById("player-frame").src = "https://m.youtube.com";
  showView("view-player");
  setBackDisabled();
}

// ====== পিন ======
function verifyPin(inputPin) {
  return inputPin === PARENT_PIN;
}

function openPinModal(intent) {
  pinIntent = intent;
  document.getElementById("pin-input").value = "";
  document.getElementById("pin-error").hidden = true;
  document.getElementById("modal-pin").classList.add("open");
  document.getElementById("pin-input").focus();
}

function closePinModal() {
  document.getElementById("modal-pin").classList.remove("open");
}

function handlePinConfirm() {
  var value = document.getElementById("pin-input").value;
  if (!verifyPin(value)) {
    document.getElementById("pin-error").hidden = false;
    return;
  }
  closePinModal();
  if (pinIntent === "timer") {
    openTimerModal();
  } else if (pinIntent === "exit") {
    setBackEnabled();
    exitApp();
  }
}

// ====== টাইমার ======
var selectedMinutes = null;

function openTimerModal() {
  selectedMinutes = null;
  document.querySelectorAll(".chip").forEach(function (chip) {
    chip.classList.remove("chip-selected");
  });
  document.getElementById("modal-timer").classList.add("open");
}

function closeTimerModal() {
  document.getElementById("modal-timer").classList.remove("open");
}

function setLockTimer(minutes) {
  var unlockUntil = Date.now() + minutes * 60 * 1000;
  localStorage.setItem(STORAGE_KEY, String(unlockUntil));
}

function clearLockTimer() {
  localStorage.removeItem(STORAGE_KEY);
}

function isLockDue() {
  var until = localStorage.getItem(STORAGE_KEY);
  if (!until) return false;
  return Date.now() >= parseInt(until, 10);
}

// ====== রিলক ======
function triggerRelock() {
  document.getElementById("player-frame").src = "";
  clearLockTimer();
  setBackDisabled();
  showView("view-lock");
}

// ====== ইভেন্ট বাইন্ডিং ======
document.addEventListener("DOMContentLoaded", function () {
  document.getElementById("btn-youtube").addEventListener("click", loadYoutube);
  document.getElementById("btn-settings-gear").addEventListener("click", function () {
    openPinModal("timer");
  });
  document.getElementById("btn-lock-parent").addEventListener("click", function () {
    openPinModal("exit");
  });

  document.getElementById("pin-cancel").addEventListener("click", closePinModal);
  document.getElementById("pin-confirm").addEventListener("click", handlePinConfirm);

  document.getElementById("timer-cancel").addEventListener("click", closeTimerModal);
  document.getElementById("timer-exit-now").addEventListener("click", function () {
    closeTimerModal();
    triggerRelock();
  });

  document.querySelectorAll(".chip").forEach(function (chip) {
    chip.addEventListener("click", function () {
      document.querySelectorAll(".chip").forEach(function (c) {
        c.classList.remove("chip-selected");
      });
      chip.classList.add("chip-selected");
      selectedMinutes = parseInt(chip.dataset.minutes, 10);
      setLockTimer(selectedMinutes);
      closeTimerModal();
    });
  });

  if (isLockDue()) {
    triggerRelock();
  }
  setInterval(function () {
    if (isLockDue()) {
      triggerRelock();
    }
  }, 5000);
});
