# Accessibility statement (honest version)

## Who this is for
**Primary goal:** convenience. SwipeSkipr scrolls YouTube Shorts hands-free for anyone who wants to watch without swiping.

**Secondary goal, stated plainly:** it may also help people with visual, motor or other physical disabilities. For example, someone with a tremor, limited hand mobility or a dexterity condition may find repeated swiping hard or tiring, and automatic advance or a flick of the phone can replace it. We want those users to be welcome and we will take their feedback seriously. They are not the first target audience and we do not market the app as a disability tool.

## What this is not
- It is **not declared as an accessibility tool**. The manifest does not set `isAccessibilityTool`, and the Play declaration (if the app is ever submitted) must not claim it. Google's policy requires that people with disabilities are the primary purpose, and it lists automation tools as not eligible (see `POLICY_NOTES.md`). Changing this is a human decision that has to be truthful.
- It is **not a replacement for a screen reader**. Blind users rely on TalkBack; this app adds no audio description or navigation. It does not make Shorts content itself more accessible.

## What is true today
- Every control has a visible text label (Compose `Text`, no icon-only buttons except the back arrow, which has a content description). Status is always written out, never colour-only.
- Layout supports right-to-left (Arabic) and nine languages; screens scroll when text is long.
- A big Start/Stop button and an always-visible Stop in the notification.
- Hands-free options: automatic interval, motion-to-next, opt-in automatic Skip.

## What has NOT been verified
- No TalkBack run, no Switch Access run, no large-font (200%) or high-contrast check, no contrast-ratio measurement, no check of touch-target sizes with a tool, no test with people who have disabilities.
- The onboarding disclosure is long text; it has not been checked for plain-language readability.
These are listed as open items. Until they are done, do not describe the app as "accessible" in store listings.

## If the audience ever shifts
Only a human can decide to target people with disabilities as the primary audience. Before that: test with TalkBack/Switch Access, involve users with disabilities, fix what they find, then reconsider the `isAccessibilityTool` declaration and the Play form honestly.
