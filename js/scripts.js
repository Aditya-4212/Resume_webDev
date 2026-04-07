document.addEventListener("DOMContentLoaded", () => {

  // Smooth scroll
  window.scrollToSection = function(id) {
    const el = document.getElementById(id);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' });
      document.querySelectorAll('.tree-item').forEach(i => i.classList.remove('active'));
    }
  };

  // Fade-up animation
  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('visible');
      }
    });
  }, { threshold: 0.1 });

  document.querySelectorAll('.fade-up').forEach(el => observer.observe(el));

  // Tabs
  document.querySelectorAll('.tab').forEach(tab => {
    tab.addEventListener('click', function(e) {
      if (e.target.classList.contains('close')) return;
      document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
      this.classList.add('active');
    });
  });

  // Copy toggle
  document.querySelector('.copy_toggle')?.addEventListener('click', function() {
    const svg = this.querySelector('svg');
    if (svg) {
      svg.style.color = 'deepskyblue';
      setTimeout(() => { svg.style.color = ''; }, 2000);
    }
  });

  // Scroll + status bar
  const mainContent = document.getElementById('main-content');
  if (mainContent) {
    mainContent.addEventListener('scroll', () => {
      const scrollPct = mainContent.scrollTop / (mainContent.scrollHeight - mainContent.clientHeight);
      const line = Math.floor(scrollPct * 500) + 1;

      const status = document.querySelector('.statusbar-right span:nth-child(3)');
      if (status) {
        status.textContent = `Ln ${line}, Col 1`;
      }
    });

    // Minimap
    const viewport = document.querySelector('.mm-viewport');
    const minimap = document.querySelector('.minimap');

    if (viewport && minimap) {
      mainContent.addEventListener('scroll', () => {
        const pct = mainContent.scrollTop / (mainContent.scrollHeight - mainContent.clientHeight);
        const maxTop = minimap.clientHeight - 60;
        viewport.style.top = (8 + pct * maxTop) + 'px';
      });
    }
  }

  // LOGIN
  const form = document.getElementById("login-form");
  const loginBtn = document.getElementById("login-btn");

  if (form && loginBtn) {
    form.addEventListener("submit", () => {
      loginBtn.classList.add("loading");
      loginBtn.disabled = true;
    });
  }

});

// LOGOUT (outside DOMContentLoaded)
function logout() {
  window.location.href = "LogoutServlet";
}
