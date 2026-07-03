/* ================================================================
   BlockPrint Cat — main.js
   Block grid, particles, mouse glow, tilts, reveals,
   scroll progress, parallax, back-to-top, hero CTA magnet.
   ================================================================ */
(function() {
  var mcColors = [
    '#7C9E4C','#8B6914','#7F7F7F','#B8945C','#3DD0D0','#FCDB58',
    '#AA0000','#214BA4','#17DD62','#150F24','#3B6BD4','#A2A284',
    '#C6C6C6','#F27FA5','#E88935','#453226'
  ];
  var particleColors = ['#FCDB58', '#F59E0B', '#D9774C', '#7CBE4F', '#22D3EE', '#A78BFA'];
  var prefersReduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  /* ----- 1. Block grid ----- */
  var grid = document.getElementById('blockGrid');
  var blockCells = [], cols, rows, total;

  function buildGrid() {
    if (!grid) return;
    grid.innerHTML = '';
    blockCells = [];
    cols = Math.ceil(window.innerWidth / 48);
    rows = Math.ceil(window.innerHeight / 48);
    total = cols * rows;
    grid.style.gridTemplateColumns = 'repeat(' + cols + ', 48px)';
    grid.style.gridTemplateRows = 'repeat(' + rows + ', 48px)';
    for (var i = 0; i < total; i++) {
      var c = document.createElement('div');
      c.className = 'block-cell';
      c.style.backgroundColor = mcColors[Math.floor(Math.random() * mcColors.length)];
      blockCells.push(c);
      grid.appendChild(c);
    }
  }
  buildGrid();

  function updateCell(idx, color) {
    if (blockCells[idx]) blockCells[idx].style.backgroundColor = color || mcColors[Math.floor(Math.random() * mcColors.length)];
  }
  function waveUpdate() {
    var cx = Math.floor(Math.random() * cols);
    var cy = Math.floor(Math.random() * rows);
    var radius = 2 + Math.floor(Math.random() * 3);
    var color = mcColors[Math.floor(Math.random() * mcColors.length)];
    var delay = 0;
    for (var dy = -radius; dy <= radius; dy++) {
      for (var dx = -radius; dx <= radius; dx++) {
        if (dx * dx + dy * dy <= radius * radius) {
          var x = cx + dx, y = cy + dy;
          if (x >= 0 && x < cols && y >= 0 && y < rows) {
            (function(idx, col) {
              setTimeout(function() { updateCell(idx, col); }, delay);
            })(y * cols + x, color);
          }
        }
      }
      delay += 40;
    }
  }
  function gridTick() {
    if (Math.random() < 0.3) waveUpdate();
    else updateCell(Math.floor(Math.random() * total));
    window.__bgTimer = setTimeout(gridTick, 500 + Math.random() * 1500);
  }
  if (!prefersReduced) window.__bgTimer = setTimeout(gridTick, 800);

  /* ----- 2. Parallax on hero grid ----- */
  function parallaxGrid() {
    if (!grid || prefersReduced) return;
    var scrollY = window.scrollY || window.pageYOffset;
    grid.style.transform = 'translateY(' + (scrollY * 0.35) + 'px)';
  }

  /* ----- 3. Hero mouse glow ----- */
  var glow = document.getElementById('heroGlow');
  var glowTimer;
  function updateGlow(e) {
    if (!glow || prefersReduced) return;
    var x = e.clientX, y = e.clientY;
    var vw = window.innerWidth, vh = window.innerHeight;
    var inHero = (x >= 0 && x <= vw && y >= 0 && y <= vh);
    glow.style.opacity = inHero ? '1' : '0';
    if (inHero) {
      glow.style.left = x + 'px';
      glow.style.top = y + 'px';
    }
  }
  document.addEventListener('mousemove', function(e) {
    if (prefersReduced) return;
    if (!glowTimer) {
      glowTimer = requestAnimationFrame(function() {
        glowTimer = null;
        updateGlow(e);
      });
    }
  });

  /* ----- 4. Screenshot card 3D tilt ----- */
  document.querySelectorAll('.screenshot-card').forEach(function(card) {
    card.addEventListener('mousemove', function(e) {
      if (prefersReduced) return;
      var rect = card.getBoundingClientRect();
      var cx = (e.clientX - rect.left) / rect.width - 0.5;
      var cy = (e.clientY - rect.top) / rect.height - 0.5;
      card.style.transform = 'perspective(800px) rotateY(' + (cx * 5) + 'deg) rotateX(' + (-cy * 5) + 'deg)';
    });
    card.addEventListener('mouseleave', function() { card.style.transform = ''; });
  });

  /* ----- 5. Tech card mouse-follow + 3D tilt ----- */
  document.querySelectorAll('.tech-item').forEach(function(card) {
    card.addEventListener('mousemove', function(e) {
      if (prefersReduced) return;
      var rect = card.getBoundingClientRect();
      card.style.setProperty('--mx', ((e.clientX - rect.left) / rect.width * 100) + '%');
      card.style.setProperty('--my', ((e.clientY - rect.top) / rect.height * 100) + '%');
      var cx = (e.clientX - rect.left) / rect.width - 0.5;
      var cy = (e.clientY - rect.top) / rect.height - 0.5;
      card.style.transform = 'perspective(800px) rotateY(' + (cx * 4) + 'deg) rotateX(' + (-cy * 4) + 'deg) translateY(-4px)';
    });
    card.addEventListener('mouseleave', function() { card.style.transform = ''; });
  });

  /* ----- 6. Hero CTA magnet effect ----- */
  // Buttons in hero gently tilt toward cursor when hovered
  document.querySelectorAll('.hero-actions .btn').forEach(function(btn) {
    btn.addEventListener('mousemove', function(e) {
      if (prefersReduced) return;
      var rect = btn.getBoundingClientRect();
      var x = (e.clientX - rect.left) / rect.width - 0.5;
      var y = (e.clientY - rect.top) / rect.height - 0.5;
      btn.style.transform = 'translate(' + (x * 6) + 'px, ' + (y * 4) + 'px)';
    });
    btn.addEventListener('mouseleave', function() {
      btn.style.transform = '';
    });
  });

  /* ----- 7. IntersectionObserver: reveal + workflow steps + dividers ----- */
  var revealObserver = new IntersectionObserver(function(entries) {
    entries.forEach(function(entry) {
      if (!entry.isIntersecting) return;
      var el = entry.target;
      if (el.classList.contains('reveal')) {
        el.classList.add('visible');
        revealObserver.unobserve(el);
      }
      if (el.classList.contains('reveal-stagger')) {
        var kids = el.children;
        for (var i = 0; i < kids.length; i++) kids[i].classList.add('visible');
        revealObserver.unobserve(el);
      }
      if (el.classList.contains('section-divider')) {
        el.classList.add('visible');
        revealObserver.unobserve(el);
      }
      if (el.classList.contains('workflow-step')) {
        el.classList.add('visible');
        revealObserver.unobserve(el);
      }
    });
  }, { threshold: 0.12, rootMargin: '0px 0px -30px 0px' });
  document.querySelectorAll('.reveal, .reveal-stagger, .section-divider, .workflow-step').forEach(function(el) {
    revealObserver.observe(el);
  });

  /* ----- 8. Scroll progress + parallax + back-to-top ----- */
  var progBar = document.getElementById('scrollProgress');
  var backToTop = document.getElementById('backToTop');
  var scrollTicking = false;

  function onScroll() {
    var st = window.scrollY;
    var docH = document.documentElement.scrollHeight - window.innerHeight;
    if (progBar) progBar.style.width = docH > 0 ? (st / docH * 100) + '%' : '0%';
    if (backToTop) {
      if (st > window.innerHeight * 0.8) backToTop.classList.add('visible');
      else backToTop.classList.remove('visible');
    }
    parallaxGrid();
  }

  window.addEventListener('scroll', function() {
    if (!scrollTicking) {
      requestAnimationFrame(onScroll);
      scrollTicking = true;
      setTimeout(function() { scrollTicking = false; }, 16);
    }
  }, { passive: true });

  /* ----- 9. Back-to-top click ----- */
  if (backToTop) {
    backToTop.addEventListener('click', function() {
      window.scrollTo({ top: 0, behavior: prefersReduced ? 'auto' : 'smooth' });
    });
  }

  /* ----- 10. Resize ----- */
  var resizeTimeout;
  window.addEventListener('resize', function() {
    clearTimeout(resizeTimeout);
    resizeTimeout = setTimeout(function() {
      buildGrid();
      if (!prefersReduced) window.__bgTimer = setTimeout(gridTick, 800);
    }, 400);
  });

  /* ----- 11. Hero particles (canvas) ----- */
  (function() {
    var canvas = document.getElementById('heroParticles');
    if (!canvas || prefersReduced) return;
    var ctx = canvas.getContext('2d');
    var dpr = Math.min(window.devicePixelRatio || 1, 2);
    var w = 0, h = 0;
    var particles = [];

    function resize() {
      var rect = canvas.parentElement.getBoundingClientRect();
      w = rect.width; h = rect.height;
      canvas.width = w * dpr;
      canvas.height = h * dpr;
      canvas.style.width = w + 'px';
      canvas.style.height = h + 'px';
      ctx.setTransform(1, 0, 0, 1, 0, 0);
      ctx.scale(dpr, dpr);
    }

    function makeParticle(sparkle) {
      return {
        x: Math.random() * w,
        y: Math.random() * h,
        size: sparkle ? 2 + Math.random() * 2 : 1 + Math.random() * 2,
        speedY: -(0.06 + Math.random() * 0.5),
        swaySpeed: 0.01 + Math.random() * 0.02,
        swayAmp: 0.2 + Math.random() * 0.4,
        phase: Math.random() * Math.PI * 2,
        opacity: sparkle ? (0.3 + Math.random() * 0.4) : (0.1 + Math.random() * 0.2),
        color: particleColors[Math.floor(Math.random() * particleColors.length)],
        sparkle: !!sparkle,
        pulsePhase: Math.random() * Math.PI * 2
      };
    }

    resize();
    for (var i = 0; i < 26; i++) particles.push(makeParticle(i < 6));

    function step() {
      ctx.clearRect(0, 0, w, h);
      var now = Date.now() * 0.001;
      for (var i = 0; i < particles.length; i++) {
        var p = particles[i];
        p.phase += p.swaySpeed;
        p.y += p.speedY;
        p.x += Math.sin(p.phase) * p.swayAmp * 0.3;
        if (p.y < -10) {
          p.x = Math.random() * w;
          p.y = h + 10;
          p.color = particleColors[Math.floor(Math.random() * particleColors.length)];
        }
        if (p.x < -10) p.x = w + 10;
        if (p.x > w + 10) p.x = -10;
        var a = p.opacity;
        if (p.sparkle) a *= (0.4 + 0.6 * Math.sin(now * 1.5 + p.pulsePhase));
        ctx.fillStyle = p.color;
        ctx.globalAlpha = a;
        ctx.fillRect(p.x, p.y, p.size, p.size);
        if (p.sparkle) {
          ctx.globalAlpha = a * 0.35;
          ctx.fillRect(p.x - 1, p.y - 0.5, p.size + 2, p.size + 1);
          ctx.fillRect(p.x - 0.5, p.y - 1, p.size + 1, p.size + 2);
        }
      }
      requestAnimationFrame(step);
    }
    step();

    var lastR = 0;
    window.addEventListener('resize', function() {
      var now = Date.now();
      if (now - lastR < 300) return;
      lastR = now;
      resize();
    });
  })();
})();
