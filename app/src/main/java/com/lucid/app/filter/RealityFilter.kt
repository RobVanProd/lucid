package com.lucid.app.filter

/**
 * The Reality Filter - DOM Sanitization Engine
 *
 * "The SLM reads the DOM before it renders. It extracts the ingredients and instructions.
 * It renders only that. Elegantly. The noise never touches your retina."
 *
 * This JavaScript is injected into every page to strip away:
 * - Advertisements
 * - Pop-ups and modals
 * - Social media embeds
 * - Newsletter sign-ups
 * - Cookie consent banners
 * - Infinite scroll triggers
 * - Recommendation engines
 * - Notification prompts
 * - Autoplay videos
 * - Comments sections (optional)
 */
object RealityFilter {

    /**
     * Master filter script - injected on page load
     */
    val LUCID_FILTER_SCRIPT = """
        (function() {
            'use strict';

            // LUCID Configuration
            const LUCID = {
                enabled: true,
                mode: 'lucid', // 'lucid' or 'explore'
                intention: '',

                // Element selectors to remove
                removeSelectors: [
                    // Ads
                    '[class*="ad-"]', '[class*="ads-"]', '[class*="advertisement"]',
                    '[id*="ad-"]', '[id*="ads-"]', '[id*="advertisement"]',
                    'ins.adsbygoogle', '.adsbygoogle', '[data-ad]',
                    'iframe[src*="doubleclick"]', 'iframe[src*="googlesyndication"]',
                    '[aria-label*="advertisement" i]',

                    // Pop-ups and modals
                    '[class*="popup"]', '[class*="modal"]', '[class*="overlay"]',
                    '[class*="lightbox"]', '[id*="popup"]', '[id*="modal"]',
                    '[role="dialog"]', '[aria-modal="true"]',

                    // Newsletter and subscription prompts
                    '[class*="newsletter"]', '[class*="subscribe"]', '[class*="signup"]',
                    '[id*="newsletter"]', '[id*="subscribe"]',
                    '[class*="email-capture"]', '[class*="lead-gen"]',

                    // Cookie banners
                    '[class*="cookie"]', '[class*="consent"]', '[class*="gdpr"]',
                    '[id*="cookie"]', '[id*="consent"]', '#onetrust-consent-sdk',
                    '.cc-window', '#CybotCookiebotDialog',

                    // Social widgets
                    '[class*="social-share"]', '[class*="share-buttons"]',
                    '.fb-like', '.twitter-share', '[class*="follow-us"]',

                    // Comments (often toxic/distracting)
                    '[id*="comments"]', '[class*="comments-section"]',
                    '#disqus_thread', '.disqus', '[data-component="comments"]',

                    // Recommendation engines
                    '[class*="recommended"]', '[class*="related-posts"]',
                    '[class*="you-may-also"]', '[class*="more-from"]',
                    '[class*="trending"]', '[class*="popular-posts"]',
                    '[class*="taboola"]', '[class*="outbrain"]',

                    // Notification prompts
                    '[class*="notification-prompt"]', '[class*="push-notification"]',
                    '[class*="bell-icon"]', '[class*="notify-me"]',

                    // Sticky elements and floating bars
                    '[class*="sticky-"]', '[class*="floating-"]',
                    '[class*="fixed-bottom"]', '[class*="fixed-header"]',

                    // Video autoplay containers
                    '[class*="autoplay"]', '[data-autoplay]',

                    // Misc distractions
                    '[class*="promo"]', '[class*="banner"]',
                    '[class*="announcement"]', '[class*="alert-bar"]',
                    '[class*="breaking-news"]'
                ],

                // Styles to apply in LUCID mode
                lucidStyles: `
                    * {
                        animation: none !important;
                        transition: none !important;
                    }
                    body {
                        filter: grayscale(100%) !important;
                        background: #FAFAFA !important;
                    }
                    img, video, iframe {
                        filter: grayscale(100%) !important;
                    }
                    a {
                        color: #2D2D2D !important;
                    }
                    ::selection {
                        background: #E0E0E0 !important;
                    }
                `
            };

            // Remove distracting elements
            function purge() {
                LUCID.removeSelectors.forEach(selector => {
                    try {
                        document.querySelectorAll(selector).forEach(el => {
                            el.remove();
                        });
                    } catch(e) {}
                });
            }

            // Apply LUCID visual style (E-ink aesthetic)
            function applyLucidStyle() {
                if (LUCID.mode !== 'lucid') return;

                let styleEl = document.getElementById('lucid-style');
                if (!styleEl) {
                    styleEl = document.createElement('style');
                    styleEl.id = 'lucid-style';
                    document.head.appendChild(styleEl);
                }
                styleEl.textContent = LUCID.lucidStyles;
            }

            // Block new distracting elements
            function observeDOM() {
                const observer = new MutationObserver((mutations) => {
                    mutations.forEach(mutation => {
                        mutation.addedNodes.forEach(node => {
                            if (node.nodeType === 1) { // Element node
                                // Check if new element matches removal selectors
                                LUCID.removeSelectors.forEach(selector => {
                                    try {
                                        if (node.matches && node.matches(selector)) {
                                            node.remove();
                                        }
                                        node.querySelectorAll && node.querySelectorAll(selector).forEach(el => el.remove());
                                    } catch(e) {}
                                });
                            }
                        });
                    });
                });

                observer.observe(document.body, {
                    childList: true,
                    subtree: true
                });
            }

            // Disable infinite scroll
            function disableInfiniteScroll() {
                // Override scroll event handlers
                const originalAddEventListener = EventTarget.prototype.addEventListener;
                EventTarget.prototype.addEventListener = function(type, listener, options) {
                    if (type === 'scroll' && LUCID.mode === 'lucid') {
                        // Allow scroll but block infinite scroll patterns
                        const wrappedListener = function(e) {
                            // Detect infinite scroll trigger (near bottom)
                            const nearBottom = window.innerHeight + window.scrollY >= document.body.offsetHeight - 500;
                            if (!nearBottom) {
                                listener.call(this, e);
                            }
                        };
                        return originalAddEventListener.call(this, type, wrappedListener, options);
                    }
                    return originalAddEventListener.call(this, type, listener, options);
                };
            }

            // Block autoplay
            function blockAutoplay() {
                // Pause all videos
                document.querySelectorAll('video').forEach(video => {
                    video.pause();
                    video.autoplay = false;
                    video.removeAttribute('autoplay');
                });

                // Override play method
                const originalPlay = HTMLMediaElement.prototype.play;
                HTMLMediaElement.prototype.play = function() {
                    // Only allow play if user initiated
                    if (!this.hasAttribute('data-lucid-user-initiated')) {
                        console.log('[LUCID] Blocked autoplay');
                        return Promise.reject(new Error('Autoplay blocked by LUCID'));
                    }
                    return originalPlay.call(this);
                };
            }

            // Recipe extraction for food sites
            function extractRecipe() {
                const recipe = {
                    title: '',
                    ingredients: [],
                    instructions: []
                };

                // Try JSON-LD first
                const jsonLd = document.querySelector('script[type="application/ld+json"]');
                if (jsonLd) {
                    try {
                        const data = JSON.parse(jsonLd.textContent);
                        const recipeData = Array.isArray(data) ?
                            data.find(d => d['@type'] === 'Recipe') :
                            (data['@type'] === 'Recipe' ? data : null);

                        if (recipeData) {
                            recipe.title = recipeData.name || '';
                            recipe.ingredients = recipeData.recipeIngredient || [];
                            recipe.instructions = (recipeData.recipeInstructions || []).map(i =>
                                typeof i === 'string' ? i : i.text || ''
                            );
                            return recipe;
                        }
                    } catch(e) {}
                }

                return null;
            }

            // Main initialization
            function init() {
                console.log('[LUCID] Reality Filter activated');

                // Initial purge
                purge();

                // Apply style
                applyLucidStyle();

                // Block autoplay
                blockAutoplay();

                // Start observing for new elements
                if (document.body) {
                    observeDOM();
                } else {
                    document.addEventListener('DOMContentLoaded', observeDOM);
                }

                // Purge again after load
                window.addEventListener('load', () => {
                    purge();
                    applyLucidStyle();
                });

                // Periodic cleanup
                setInterval(purge, 2000);
            }

            // Expose to Android
            window.LUCID = {
                setMode: function(mode) {
                    LUCID.mode = mode;
                    if (mode === 'lucid') {
                        applyLucidStyle();
                    } else {
                        const styleEl = document.getElementById('lucid-style');
                        if (styleEl) styleEl.remove();
                    }
                    purge();
                },
                setIntention: function(intention) {
                    LUCID.intention = intention;
                },
                extractRecipe: extractRecipe,
                purge: purge
            };

            // Initialize
            init();
        })();
    """.trimIndent()

    /**
     * Content extraction script for reader mode
     */
    val READER_MODE_SCRIPT = """
        (function() {
            // Extract main content using readability heuristics
            function extractContent() {
                const candidates = [];
                const content = document.querySelectorAll('article, [role="main"], main, .post-content, .article-content, .entry-content');

                if (content.length > 0) {
                    return content[0].innerHTML;
                }

                // Fallback: find largest text block
                document.querySelectorAll('div, section').forEach(el => {
                    const text = el.innerText || '';
                    if (text.length > 500) {
                        candidates.push({
                            el: el,
                            score: text.length
                        });
                    }
                });

                candidates.sort((a, b) => b.score - a.score);
                return candidates[0]?.el?.innerHTML || document.body.innerHTML;
            }

            return extractContent();
        })();
    """.trimIndent()
}
