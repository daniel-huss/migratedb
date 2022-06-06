(()=>{
    const check = (script)=> {
      if (script.getAttribute('src').includes('analytics') || script.textContent.includes('analytics')) {
        script.textContent = '';
        script.remove();
      }
    };
    new MutationObserver((mutations, observer) => {
      for(const mutation of mutations) {
        if (mutation.type === 'childList') {
            for(const added of mutation.addedNodes) {
                if (added.localName === 'script') {
                    check(added)
                }
            }
        }
      }
    }).observe(document.body, { childList: true });
    document.body.querySelectorAll('script').forEach(check)
})();
