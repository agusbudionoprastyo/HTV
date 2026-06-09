/* ==========================================================================
   HTV Promo Landing Page Interactions (Refined Indonesian Version)
   ========================================================================== */

document.addEventListener('DOMContentLoaded', () => {
  
  // 1. Navigation Scroll Effect
  const navbar = document.querySelector('.navbar');
  window.addEventListener('scroll', () => {
    if (window.scrollY > 50) {
      navbar.classList.add('scrolled');
    } else {
      navbar.classList.remove('scrolled');
    }
  });

  // 2. Interactive TV Gallery State Handler (Full Autoplay & Refined Shadows)
  const navTabs = document.querySelectorAll('.simulator-tab-btn');
  const screenImages = document.querySelectorAll('.simulator-screen-img');
  const infoBox = document.querySelector('.simulator-info-box');
  const infoTitle = document.getElementById('simulator-info-title');
  const infoDesc = document.getElementById('simulator-info-desc');

  // Flat Configuration representing all screen states in order
  const autoplayFlow = [
    {
      tab: 'screen-beranda',
      screen: 'screen-beranda',
      title: 'Dashboard TV Interaktif Terpadu',
      desc: 'Tampilan beranda utama yang bersih dengan akses langsung ke seluruh layanan hotel, widget cuaca, banner promo, dan jadwal penerbangan real-time (FIDS).'
    },
    {
      tab: 'screen-welcome',
      screen: 'screen-welcome',
      title: 'Sambutan Tamu Personal yang Hangat',
      desc: 'Menyambut tamu dengan sapaan khusus, nomor kamar, dan profil personal saat pertama kali memasuki kamar, menciptakan impresi pertama yang eksklusif.'
    },
    {
      tab: 'screen-fnb-flow',
      screen: 'screen-fnb',
      title: 'Menu Makanan & Minuman Digital (F&B)',
      desc: 'Tamu dapat menjelajahi menu kuliner premium hotel dengan foto resolusi tinggi, harga, dan deskripsi lengkap langsung dari TV kamar.'
    },
    {
      tab: 'screen-fnb-flow',
      screen: 'screen-cart',
      title: 'Keranjang Pemesanan Digital',
      desc: 'Memudahkan tamu meninjau hidangan yang dipilih, menyesuaikan jumlah pesanan, dan melihat total tagihan sementara sebelum checkout.'
    },
    {
      tab: 'screen-fnb-flow',
      screen: 'screen-ordersummary',
      title: 'Ringkasan & Konfirmasi Order',
      desc: 'Halaman verifikasi akhir. Begitu dikonfirmasi, pesanan akan dikirim ke dapur dan tagihan otomatis dibebankan ke akun folio kamar PMS tamu.'
    },
    {
      tab: 'screen-fnb-flow',
      screen: 'screen-myorder',
      title: 'Pelacakan Status Pesanan',
      desc: 'Tamu dapat melihat riwayat dan memantau status hidangan secara real-time: Mulai dari Diterima, Sedang Dimasak, hingga Siap Diantar.'
    },
    {
      tab: 'screen-hotelinfo',
      screen: 'screen-hotelinfo',
      title: 'Panduan Lengkap Fasilitas Hotel',
      desc: 'Informasi digital seputar jam operasional kolam renang, spa, gym, fasilitas rapat, hingga panduan wisata lokal tanpa memerlukan brosur kertas.'
    },
    {
      tab: 'screen-request',
      screen: 'screen-request',
      title: 'Permintaan Perlengkapan Kamar Mandiri',
      desc: 'Minta handuk ekstra, air mineral tambahan, bantal, atau pembersihan kamar dengan cepat lewat remote TV. Permintaan langsung masuk ke sistem house-keeping.'
    },
    {
      tab: 'screen-dnd',
      screen: 'screen-dnd',
      title: 'Mode Privasi Jangan Ganggu (DND)',
      desc: 'Aktifkan status DND dari TV untuk ketenangan istirahat Anda. Status disinkronkan langsung ke dasbor staf hotel secara instan.'
    },
    {
      tab: 'screen-appdrawer',
      screen: 'screen-appdrawer',
      title: 'Pusat Pintasan Aplikasi Populer',
      desc: 'Akses mudah ke aplikasi hiburan favorit tamu seperti YouTube, Netflix, Prime Video, dan aplikasi pihak ketiga lainnya yang diinstal pada smart TV.'
    },
    {
      tab: 'screen-settingpin',
      screen: 'screen-settingpin',
      title: 'Proteksi Parameter & Keamanan TV',
      desc: 'Pengamanan menu administrator, setelan jaringan, Firebase DB, dan konfigurasi sensitif lainnya menggunakan verifikasi kode PIN keamanan khusus.'
    }
  ];

  let autoplayInterval = null;
  let currentIndex = 0;
  const cycleDuration = 4500; // 4.5 seconds loop speed

  function resetAllProgressRings() {
    navTabs.forEach(tab => {
      const circle = tab.querySelector('.progress-ring__circle');
      if (circle) {
        circle.style.transition = 'none';
        circle.style.strokeDashoffset = '163.4';
      }
    });
  }

  function startProgressRing(activeTab, currentStep) {
    resetAllProgressRings();
    if (!activeTab || !currentStep) return;
    
    const circle = activeTab.querySelector('.progress-ring__circle');
    if (circle) {
      // Find how many steps are in this active tab group
      const tabSteps = autoplayFlow.filter(step => step.tab === currentStep.tab);
      const numSteps = tabSteps.length;
      const stepIndexInTab = tabSteps.findIndex(step => step.screen === currentStep.screen);
      
      const circumference = 163.4;
      
      // Calculate start and end offsets for the current slide segment
      const startOffset = circumference * (1 - stepIndexInTab / numSteps);
      const endOffset = circumference * (1 - (stepIndexInTab + 1) / numSteps);
      
      // 1. Reset offset instantly to startOffset
      circle.style.transition = 'none';
      circle.style.strokeDashoffset = `${startOffset}`;
      
      // 2. Force browser layout reflow
      circle.getBoundingClientRect();
      
      // 3. Trigger linear stroke fill transition matching loop duration
      circle.style.transition = `stroke-dashoffset ${cycleDuration}ms linear`;
      circle.style.strokeDashoffset = `${endOffset}`;
    }
  }

  function showActiveStep(index) {
    const currentStep = autoplayFlow[index];
    if (!currentStep) return;

    // Find and highlight active tab button
    let activeTab = null;
    navTabs.forEach(tab => {
      if (tab.getAttribute('data-screen') === currentStep.tab) {
        tab.classList.add('active');
        activeTab = tab;
      } else {
        tab.classList.remove('active');
      }
    });

    // Update TV screen image
    screenImages.forEach(img => {
      if (img.getAttribute('id') === currentStep.screen) {
        img.classList.add('active');
      } else {
        img.classList.remove('active');
      }
    });

    // Update details text with smooth fade transition
    if (infoBox && infoTitle && infoDesc) {
      infoBox.classList.add('fade-out');
      setTimeout(() => {
        infoTitle.textContent = currentStep.title;
        infoDesc.textContent = currentStep.desc;
        infoBox.classList.remove('fade-out');
      }, 300);
    }

    // Start progress ring fill on the highlighted tab button
    startProgressRing(activeTab, currentStep);
  }

  function stopAutoplay() {
    if (autoplayInterval) {
      clearInterval(autoplayInterval);
      autoplayInterval = null;
    }
    resetAllProgressRings();
  }

  function startAutoplay() {
    stopAutoplay();
    showActiveStep(currentIndex);

    autoplayInterval = setInterval(() => {
      currentIndex = (currentIndex + 1) % autoplayFlow.length;
      showActiveStep(currentIndex);
    }, cycleDuration);
  }

  // Set up manual click events to jump the autoplay index
  navTabs.forEach(tab => {
    tab.addEventListener('click', () => {
      const clickedTabScreen = tab.getAttribute('data-screen');
      
      // Find the first step index associated with this tab
      const targetIndex = autoplayFlow.findIndex(step => step.tab === clickedTabScreen);
      if (targetIndex !== -1) {
        stopAutoplay();
        currentIndex = targetIndex;
        startAutoplay();
      }
    });
  });

  // Start the autoplay loop initially
  startAutoplay();

  // 3. Scroll Reveal Animation using IntersectionObserver
  const revealElements = document.querySelectorAll('.reveal');
  
  const revealObserver = new IntersectionObserver((entries, observer) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('active');
        // Unobserve once animated to keep animation clean
        observer.unobserve(entry.target);
      }
    });
  }, {
    threshold: 0.15,
    rootMargin: '0px 0px -50px 0px'
  });

  revealElements.forEach(el => {
    revealObserver.observe(el);
  });

  // 4. Contact Form Submission (Redirect to WhatsApp)
  const contactForm = document.getElementById('demo-request-form');
  const formStatus = document.getElementById('form-status-message');

  if (contactForm) {
    contactForm.addEventListener('submit', (e) => {
      e.preventDefault();

      // Retrieve form values
      const name = document.getElementById('form-name').value.trim();
      const email = document.getElementById('form-email').value.trim();
      const hotel = document.getElementById('form-hotel').value.trim();
      const message = document.getElementById('form-message').value.trim();

      // Basic Validation
      if (!name || !email || !hotel) {
        showStatus('Mohon lengkapi semua kolom yang wajib diisi.', 'error');
        return;
      }

      // Show submitting state
      const submitBtn = contactForm.querySelector('button[type="submit"]');
      const originalText = submitBtn.textContent;
      submitBtn.textContent = 'Menghubungkan ke WhatsApp...';
      submitBtn.disabled = true;

      // Construct WhatsApp message
      const introText = "Halo Tim HTV, saya ingin mengajukan permintaan demo:";
      const nameLine = `- Nama Lengkap: ${name}`;
      const emailLine = `- Email Kerja: ${email}`;
      const hotelLine = `- Nama Hotel/Properti: ${hotel}`;
      const msgLine = message ? `- Kebutuhan Khusus: ${message}` : "";
      
      const fullMessageText = [introText, nameLine, emailLine, hotelLine, msgLine].filter(Boolean).join("\n");
      const encodedText = encodeURIComponent(fullMessageText);
      const whatsappUrl = `https://wa.me/6285695022093?text=${encodedText}`;

      // Mock delay before redirecting
      setTimeout(() => {
        submitBtn.textContent = originalText;
        submitBtn.disabled = false;
        
        // Open WhatsApp in a new tab
        window.open(whatsappUrl, '_blank');
        
        // Show success status on page
        showStatus(`Terima kasih, ${name}! Permintaan demo Anda sedang dialihkan ke WhatsApp kami...`, 'success');
        contactForm.reset();
      }, 1000);
    });
  }

  function showStatus(msg, type) {
    formStatus.textContent = msg;
    formStatus.className = 'form-status'; // Reset classes
    
    if (type === 'success') {
      formStatus.classList.add('success');
    } else {
      formStatus.classList.add('error');
    }

    // Scroll status message into view smoothly
    formStatus.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }
});
