  // Smooth scroll to sections
  function scrollTo(id) {
    const el = document.getElementById(id);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' });
      // Update active tab
      document.querySelectorAll('.tree-item').forEach(i => i.classList.remove('active'));
    }
  }

  // Fade-up animation on scroll
  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('visible');
      }
    });
  }, { threshold: 0.1 });

  document.querySelectorAll('.fade-up').forEach(el => observer.observe(el));

  // Tab click behavior
  document.querySelectorAll('.tab').forEach(tab => {
    tab.addEventListener('click', function(e) {
      if (e.target.classList.contains('close')) return;
      document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
      this.classList.add('active');
    });
  });

  // Copy toggle button
  document.querySelector('.copy_toggle')?.addEventListener('click', function() {
    const svg = this.querySelector('svg');
    svg.style.color = 'deepskyblue';
    setTimeout(() => { svg.style.color = ''; }, 2000);
  });

  // Statusbar line/col update on scroll
  const mainContent = document.getElementById('main-content');
  mainContent.addEventListener('scroll', () => {
    const scrollPct = mainContent.scrollTop / (mainContent.scrollHeight - mainContent.clientHeight);
    const line = Math.floor(scrollPct * 500) + 1;
    document.querySelector('.statusbar-right span:nth-child(3)').textContent = `Ln ${line}, Col 1`;
  });

  // Minimap viewport drag effect
  const viewport = document.querySelector('.mm-viewport');
  mainContent.addEventListener('scroll', () => {
    const pct = mainContent.scrollTop / (mainContent.scrollHeight - mainContent.clientHeight);
    const minimap = document.querySelector('.minimap');
    const maxTop = minimap.clientHeight - 60;
    viewport.style.top = (8 + pct * maxTop) + 'px';
  });