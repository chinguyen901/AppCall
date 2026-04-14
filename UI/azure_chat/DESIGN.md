# Design System Documentation: The Fluid Connection

## 1. Overview & Creative North Star
The Creative North Star for this design system is **"The Translucent Stream."** 

While inspired by the immediacy of Facebook Messenger, this system evolves the "standard" chat interface into a high-end, editorial communication experience. We move away from the rigid, boxed-in feel of traditional apps by embracing **Soft Minimalism**. The interface should feel like a living stream of data—fluid, continuous, and weightless. By utilizing extreme roundedness, intentional asymmetry in message pacing, and depth through tonal layering rather than lines, we create a digital environment that prioritizes the human connection over the technical container.

---

## 2. Colors & Surface Philosophy

### The "No-Line" Rule
To achieve a premium, editorial feel, **1px solid borders are strictly prohibited for sectioning.** Boundaries must be defined solely through background color shifts. For example, a chat input area should not be separated by a line; instead, use a `surface-container-low` background against the `surface` of the message thread.

### Surface Hierarchy & Nesting
Treat the UI as a series of nested, physical layers. Each tier represents a level of "focus":
- **Base Layer:** `surface` (#f7f9fc) for the main application background.
- **Content Blocks:** `surface-container-low` (#f2f4f7) for grouping message history.
- **Floating Elements:** `surface-container-lowest` (#ffffff) for active input fields or cards to create a "lifted" feel.
- **Active Overlays:** Use `surface-bright` for elements that need to pop against the soft gray background.

### The "Glass & Gradient" Rule
To avoid a "flat" or "generic" appearance, primary actions should utilize **Signature Textures**. 
- **The Messenger Blue:** Use `primary` (#005bb3) as the base, but apply a subtle linear gradient to `primary-container` (#0073df) at a 135-degree angle for main CTAs. 
- **Frosted Navigation:** The top app bar and bottom navigation should use a "Glassmorphism" effect: a semi-transparent `surface` color with a `backdrop-filter: blur(20px)`.

---

## 3. Typography: Editorial Sans
We use **Be Vietnam Pro** for its geometric clarity and contemporary warmth, moving beyond the standard Roboto to provide a more custom, "designed" feel.

- **Display & Headlines:** Use `display-md` (2.75rem) for "Welcome" states or empty chat screens. This creates a bold, editorial entry point.
- **Conversation Titles:** `title-lg` (1.375rem) provides authority to contact names in the header.
- **The Message Bubble:** `body-lg` (1rem) is the standard for chat text. It is balanced for high readability and rhythmic spacing.
- **Metadata:** `label-sm` (0.6875rem) in `on-surface-variant` is used for timestamps and "Seen" receipts, ensuring they are present but secondary.

---

## 4. Elevation & Depth

### The Layering Principle
Depth is achieved through **Tonal Layering**. Avoid shadows for static elements. Instead, place a `surface-container-lowest` card on top of a `surface-container-high` background to create a soft, natural lift.

### Ambient Shadows
When an element must "float" (e.g., a Floating Action Button or a Modal), use a custom **Ambient Shadow**:
- **Color:** A tinted version of `on-surface` (#191c1e) at 6% opacity.
- **Blur:** High diffusion (24px to 48px) with a small Y-offset (4px). This mimics natural light rather than a digital drop shadow.

### The "Ghost Border" Fallback
If a border is required for accessibility, it must be a **Ghost Border**: Use `outline-variant` (#c0c6d6) at 15% opacity. High-contrast, opaque borders are forbidden.

---

## 5. Components

### Buttons
- **Primary:** Rounded `full` (9999px). Uses the blue gradient (`primary` to `primary-container`). White text.
- **Secondary:** Surface-based. `surface-container-high` background with `primary` colored text. No border.
- **States:** On press, increase the `surface-tint` overlay by 10%.

### Message Bubbles (The Signature Component)
- **Sent:** `primary` background with `on-primary` text. Use `xl` (3rem) roundedness on three corners, with the corner pointing to the avatar using `sm` (0.5rem) to create a "speech" tail effect.
- **Received:** `surface-container-high` background with `on-surface` text.
- **Spacing:** No dividers. Use 4px vertical spacing between bubbles from the same sender and 16px between different senders.

### Rounded Input Fields
- **Container:** `full` roundedness. Background: `surface-container-highest` (#e0e3e6).
- **Focus State:** Instead of a border change, shift the background to `surface-container-lowest` (#ffffff) and apply an Ambient Shadow.

### Lists & Navigation
- **Dividers:** Forbid the use of line dividers. Separate chat threads using a 12px vertical gap and a subtle background shift on hover/touch (`surface-container-low`).
- **Avatars:** Strictly circular. Use a 2px `surface` gap (the "Halo" effect) when overlapping status indicators (Online/Busy).

---

## 6. Do's and Don'ts

### Do:
- **Use White Space as a Tool:** Treat negative space as a structural element to separate conversations.
- **Embrace Asymmetry:** Allow received and sent messages to have different horizontal padding to create a visual "path" for the eye.
- **Use Tonal Transitions:** Transition from `surface` to `surface-container-low` to define the transition from "Search" to "Recent Chats."

### Don't:
- **Never use 100% Black:** Use `on-surface` (#191c1e) for text to maintain a soft, premium look.
- **Avoid Sharp Corners:** The minimum roundedness for any interactive element is `sm` (0.5rem). Most elements should lean toward `lg` (2rem) or `full`.
- **No Heavy Dividers:** If the UI feels cluttered, increase the padding/margins rather than adding lines.