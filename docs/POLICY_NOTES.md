# Policy notes

Re-verified from primary sources on **2026-10-19** (fetched directly).

## Google Play: AccessibilityService API
Source: https://support.google.com/googleplay/android-developer/answer/10964491

- "Any use of the Accessibility API that enables an app to autonomously initiate, plan, and execute actions or decisions is strictly prohibited."
- The page adds that this "does not prohibit deterministic, rule-based automation" whose behavior follows a static set of rules. This nuance was not in the kickoff prompt. It is the argument for a user-started, bounded, rule-based session. It does not guarantee approval.
- Verified accessibility tools (`isAccessibilityTool="true"`) are exempt only when the autonomy serves the core purpose of assisting people with disabilities.
- The page lists "automation tools" among apps that are NOT accessibility tools. This app must not set `isAccessibilityTool`.
- Non-accessibility-tool apps need the Permission Declaration Form and a prominent disclosure that is separate from other disclosures (not only in the privacy policy) and describes the data accessed.

Source for the announcement: https://support.google.com/googleplay/android-developer/answer/16550159 (reinforces the same prohibition).

Consequence: `play` flavor defaults to `MANUAL_TAP`. Whether `AUTO_TIMER` may be submitted is a human decision. Similar apps existing on Play does not imply approval.

## Audience and the accessibility-tool flag
Our stance is in `ACCESSIBILITY_STATEMENT.md`: convenience first, people with visual/motor/physical disabilities a welcome secondary audience. Because the primary purpose is not assisting people with disabilities, `isAccessibilityTool` stays unset and the app must pass Play's prominent-disclosure route if ever submitted.

## IzzyOnDroid
Source: https://izzyondroid.org/docs/general/AppInclusionPolicy/

- Says it is "strongly opposed to apps which are fully or in part created by generative AI tools", and that the code itself should be free of LLM output. Rejection is likely for this project. See `DISTRIBUTION.md`.

## Android restricted settings
Behavior for sideloaded APKs is covered in `DECISIONS.md` (ADR on onboarding) with the date checked.
