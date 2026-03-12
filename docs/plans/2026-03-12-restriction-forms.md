# Restriction Forms Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Create Create and Edit screens for Restrictions with a shared form component.

**Architecture:** 
- `RestrictionForm`: A reusable component using `react-native-paper` and `react-hook-form` (or local state) to handle the complex form logic.
- `new.tsx`: Wraps `RestrictionForm` for creating.
- `[id]/edit.tsx`: Wraps `RestrictionForm` for editing, handling data fetching.
- State management: `useState` for form fields, `react-query` for data.

**Tech Stack:** React Native, Expo, React Native Paper, React Query, TypeScript.

---

### Task 1: Create Shared RestrictionForm Component

**Files:**
- Create: `src/components/restrictions/RestrictionForm.tsx`

**Step 1: Create the component skeleton**
- Define `RestrictionFormProps` interface (initialData, onSubmit, isSubmitting, error).
- Import UI components from `react-native-paper`.
- Setup local state for all fields:
  - `volunteer` (id, name)
  - `scheduleId`
  - `typeId`
  - `description`
  - `mode`
  - `weekdays` (Set<number>)
  - `periods` (Set<string>)
  - `specificDates` (Array<{date, notes}>)
  - `dateRanges` (Array<{start, end}>)

**Step 2: Implement Volunteer Search**
- Use `useSearchVolunteers` hook.
- Create a `TextInput` for search.
- Display results in a list/menu.
- Handle selection.

**Step 3: Implement Other Fields**
- Schedule: `Menu` or `Dropdown`.
- Type: `Menu` or `RadioButton`.
- Mode: `SegmentedButtons` or `Button` toggle.
- Weekdays: `Chip` toggle.
- Periods: `Chip` toggle.
- Dates/Ranges: Dynamic lists with Add/Remove buttons. Use `dayjs` for validation.

**Step 4: Implement Submit Logic**
- Validation: Check required fields.
- Construct `Restriction` object.
- Call `onSubmit`.

### Task 2: Create New Restriction Screen

**Files:**
- Create: `src/app/(tabs)/restrictions/new.tsx`

**Step 1: Implement Screen**
- Import `RestrictionForm`.
- Use `useCreateRestriction` hook.
- Handle `onSubmit` to call `mutate`.
- Handle success (go back) and error.

### Task 3: Create Edit Restriction Screen

**Files:**
- Create: `src/app/(tabs)/restrictions/[id]/edit.tsx`

**Step 1: Implement Screen**
- Get `id` from params.
- Use `useRestriction(id)` to fetch data.
- Show loading/error states.
- Parse `rules_json` string to populate form.
- Use `useUpdateRestriction` hook.
- Render `RestrictionForm` with `initialData`.

### Task 4: Verification

**Step 1: Type Check**
- Run `npx tsc --noEmit`.
- Fix any errors.

**Step 2: Test**
- Run `npx jest --no-coverage`.
- Fix any errors.
