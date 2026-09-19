// Study OS Ambient Sound Engine - Procedural Web Audio Synthesizer
// Zero external files, zero latency, seamless continuous loops for deep focus
(function() {
  var audioCtx = null;
  var currentSound = null;
  var gainNode = null;
  var currentVolume = 0.5;
  var isPlaying = false;
  var activePreset = 'none';

  function initContext() {
    if (!audioCtx) {
      var AudioContextClass = window.AudioContext || window.webkitAudioContext;
      if (AudioContextClass) {
        audioCtx = new AudioContextClass();
      }
    }
    if (audioCtx && audioCtx.state === 'suspended') {
      audioCtx.resume();
    }
  }

  function createNoiseBuffer(type, seconds) {
    if (!audioCtx) return null;
    var sampleRate = audioCtx.sampleRate;
    var bufferSize = sampleRate * seconds;
    var buffer = audioCtx.createBuffer(2, bufferSize, sampleRate);
    var left = buffer.getChannelData(0);
    var right = buffer.getChannelData(1);

    if (type === 'white') {
      for (var i = 0; i < bufferSize; i++) {
        left[i] = (Math.random() * 2 - 1) * 0.5;
        right[i] = (Math.random() * 2 - 1) * 0.5;
      }
    } else if (type === 'pink') {
      var b0 = 0, b1 = 0, b2 = 0, b3 = 0, b4 = 0, b5 = 0, b6 = 0;
      var rb0 = 0, rb1 = 0, rb2 = 0, rb3 = 0, rb4 = 0, rb5 = 0, rb6 = 0;
      for (var i = 0; i < bufferSize; i++) {
        var white = Math.random() * 2 - 1;
        b0 = 0.99886 * b0 + white * 0.0555179;
        b1 = 0.99332 * b1 + white * 0.0750759;
        b2 = 0.96900 * b2 + white * 0.1538520;
        b3 = 0.86650 * b3 + white * 0.3104856;
        b4 = 0.55000 * b4 + white * 0.5329522;
        b5 = -0.7616 * b5 - white * 0.0168980;
        left[i] = (b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362) * 0.12;
        b6 = white * 0.115926;

        var rwhite = Math.random() * 2 - 1;
        rb0 = 0.99886 * rb0 + rwhite * 0.0555179;
        rb1 = 0.99332 * rb1 + rwhite * 0.0750759;
        rb2 = 0.96900 * rb2 + rwhite * 0.1538520;
        rb3 = 0.86650 * rb3 + rwhite * 0.3104856;
        rb4 = 0.55000 * rb4 + rwhite * 0.5329522;
        rb5 = -0.7616 * rb5 - rwhite * 0.0168980;
        right[i] = (rb0 + rb1 + rb2 + rb3 + rb4 + rb5 + rb6 + rwhite * 0.5362) * 0.12;
        rb6 = rwhite * 0.115926;
      }
    } else {
      // Brown noise / Red noise
      var lastOutL = 0.0;
      var lastOutR = 0.0;
      for (var i = 0; i < bufferSize; i++) {
        var whiteL = Math.random() * 2 - 1;
        var whiteR = Math.random() * 2 - 1;
        lastOutL = (lastOutL + (0.025 * whiteL)) / 1.025;
        lastOutR = (lastOutR + (0.025 * whiteR)) / 1.025;
        left[i] = lastOutL * 2.8;
        right[i] = lastOutR * 2.8;
      }
    }
    return buffer;
  }

  function stop() {
    if (currentSound && currentSound.nodes) {
      for (var i = 0; i < currentSound.nodes.length; i++) {
        try {
          var n = currentSound.nodes[i];
          if (n.stop) n.stop();
          if (n.disconnect) n.disconnect();
        } catch(e) {}
      }
    }
    currentSound = null;
    isPlaying = false;
  }

  function play(preset, volume) {
    try {
      initContext();
      if (!audioCtx) return;
      stop();

      if (!preset || preset === 'none') {
        activePreset = 'none';
        return;
      }

      activePreset = preset;
      if (volume !== undefined && volume !== null) {
        currentVolume = Math.max(0, Math.min(1, volume));
      }

      gainNode = audioCtx.createGain();
      gainNode.gain.setValueAtTime(currentVolume, audioCtx.currentTime);
      gainNode.connect(audioCtx.destination);

      var nodes = [];

      if (preset === 'white') {
        var buffer = createNoiseBuffer('white', 6);
        var source = audioCtx.createBufferSource();
        source.buffer = buffer;
        source.loop = true;
        source.connect(gainNode);
        source.start();
        nodes.push(source);
      } else if (preset === 'pink') {
        var buffer = createNoiseBuffer('pink', 6);
        var source = audioCtx.createBufferSource();
        source.buffer = buffer;
        source.loop = true;
        source.connect(gainNode);
        source.start();
        nodes.push(source);
      } else if (preset === 'brown') {
        var buffer = createNoiseBuffer('brown', 6);
        var source = audioCtx.createBufferSource();
        source.buffer = buffer;
        source.loop = true;
        source.connect(gainNode);
        source.start();
        nodes.push(source);
      } else if (preset === 'rain') {
        var buffer = createNoiseBuffer('pink', 6);
        var source = audioCtx.createBufferSource();
        source.buffer = buffer;
        source.loop = true;

        var lowpass = audioCtx.createBiquadFilter();
        lowpass.type = 'lowpass';
        lowpass.frequency.setValueAtTime(1100, audioCtx.currentTime);

        var highpass = audioCtx.createBiquadFilter();
        highpass.type = 'highpass';
        highpass.frequency.setValueAtTime(280, audioCtx.currentTime);

        source.connect(lowpass);
        lowpass.connect(highpass);
        highpass.connect(gainNode);
        source.start();
        nodes.push(source, lowpass, highpass);
      } else if (preset === 'waves') {
        var buffer = createNoiseBuffer('brown', 8);
        var source = audioCtx.createBufferSource();
        source.buffer = buffer;
        source.loop = true;

        var lowpass = audioCtx.createBiquadFilter();
        lowpass.type = 'lowpass';
        lowpass.frequency.setValueAtTime(550, audioCtx.currentTime);

        var waveGain = audioCtx.createGain();
        waveGain.gain.setValueAtTime(0.35, audioCtx.currentTime);

        var lfo = audioCtx.createOscillator();
        lfo.type = 'sine';
        lfo.frequency.setValueAtTime(0.12, audioCtx.currentTime);

        var lfoGain = audioCtx.createGain();
        lfoGain.gain.setValueAtTime(0.30, audioCtx.currentTime);

        lfo.connect(lfoGain);
        lfoGain.connect(waveGain.gain);

        source.connect(lowpass);
        lowpass.connect(waveGain);
        waveGain.connect(gainNode);

        source.start();
        lfo.start();
        nodes.push(source, lowpass, waveGain, lfo, lfoGain);
      } else if (preset === 'stream') {
        var buffer = createNoiseBuffer('pink', 6);
        var source = audioCtx.createBufferSource();
        source.buffer = buffer;
        source.loop = true;

        var bandpass = audioCtx.createBiquadFilter();
        bandpass.type = 'bandpass';
        bandpass.frequency.setValueAtTime(750, audioCtx.currentTime);
        bandpass.Q.setValueAtTime(1.4, audioCtx.currentTime);

        var lowpass = audioCtx.createBiquadFilter();
        lowpass.type = 'lowpass';
        lowpass.frequency.setValueAtTime(2200, audioCtx.currentTime);

        source.connect(bandpass);
        bandpass.connect(lowpass);
        lowpass.connect(gainNode);
        source.start();
        nodes.push(source, bandpass, lowpass);
      }

      currentSound = { nodes: nodes, gainNode: gainNode };
      isPlaying = true;
    } catch(err) {
      console.warn('Ambient sound error:', err);
    }
  }

  function setVolume(volume) {
    currentVolume = Math.max(0, Math.min(1, volume));
    if (gainNode && audioCtx) {
      try {
        gainNode.gain.setValueAtTime(currentVolume, audioCtx.currentTime);
      } catch(e) {}
    }
  }

  window.ambientAudio = {
    play: play,
    stop: stop,
    setVolume: setVolume,
    getPreset: function() { return activePreset; },
    getVolume: function() { return currentVolume; },
    isPlaying: function() { return isPlaying; }
  };
})();
