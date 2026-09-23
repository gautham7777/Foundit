# FoundIt — Community Lost & Found Web App

A modern, responsive web application for neighbors to report found items and safely reunite them with their rightful owners.

## Features
- **Community Feed:** Browse nearby found items with category tags, approximate neighborhood areas, relative timestamps, and distance filters.
- **Privacy & Security Protection:** Detailed identification numbers (credit cards, passport numbers, home addresses) are protected. Only general public details are shown.
- **Ownership Verification Questions:** Finders set a question only the real owner can answer before claiming.
- **Private In-Browser Messaging:** Coordinate handoffs with built-in public safety reminders.
- **Smart Lost Watch Alerts:** Set alerts for missing items to be automatically notified of matching reports.
- **Mark as Returned:** Complete the lifecycle with community badge confirmations.

---

## 🚀 One-Click Deploy to Vercel

This repository is pre-configured with `vercel.json` and a production-ready web frontend in `public/`.

### Option 1: Deploy with Git (Recommended)
1. Push this project to your GitHub account (via AI Studio's **Export to GitHub** in the top-right settings).
2. Go to [vercel.com/new](https://vercel.com/new).
3. Select your imported repository.
4. Click **Deploy**. Vercel will automatically detect `vercel.json` and publish your live URL!

### Option 2: Deploy with Vercel CLI
From your terminal:
```bash
npm install -g vercel
vercel
```
Press Enter on all prompts — Vercel will immediately deploy and output your live production URL (e.g. `https://foundit-xxxx.vercel.app`).
