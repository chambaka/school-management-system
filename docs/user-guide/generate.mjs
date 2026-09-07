import { chromium } from "playwright";
import { execFileSync } from "node:child_process";
import { mkdirSync, writeFileSync, readFileSync, existsSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = join(__dirname, "..");
const SHOTS = join(__dirname, "screenshots");
const BASE = "http://127.0.0.1:5173";
const PLATFORM = { email: "halo.admin@halo-schools.net", password: "ChangeMe123!" };
const SCHOOL = { email: "guide.admin@halo-schools.net", password: "HaloGuide1!", name: "School Administrator" };
const HASH = "$2b$12$lskJILn21.DnT7TEu0ClDuKQjxFd88/sG4F8CMWcok0VNBJdVy/6O";

mkdirSync(SHOTS, { recursive: true });

function mysql(sql) {
  return execFileSync(
    "mysql",
    ["-h127.0.0.1", "-usms_user", "-pUhambule1972!", "db_school_sms", "-N", "-e", sql],
    { encoding: "utf8" },
  ).trim();
}

function ensureGuideUser() {
  mysql(`DELETE FROM refresh_tokens WHERE user_id IN (SELECT id FROM users WHERE email='${SCHOOL.email}')`);
  mysql(`DELETE FROM users WHERE email='${SCHOOL.email}'`);
  mysql(`
    INSERT INTO users (school_id, name, email, password, role, phone, enabled, created_at, updated_at, campus_id, tenant_id)
    VALUES (2, '${SCHOOL.name}', '${SCHOOL.email}', '${HASH}', 'ADMIN', NULL, 1, NOW(6), NOW(6), 2, 2)
  `);
}

function removeGuideUser() {
  try {
    mysql(`DELETE FROM refresh_tokens WHERE user_id IN (SELECT id FROM users WHERE email='${SCHOOL.email}')`);
    mysql(`DELETE FROM users WHERE email='${SCHOOL.email}'`);
  } catch (error) {
    console.warn("Could not remove guide user:", error.message);
  }
}

async function shot(page, name) {
  await page.waitForTimeout(500);
  const file = join(SHOTS, `${name}.png`);
  await page.screenshot({ path: file, fullPage: true });
  return file;
}

async function login(page, account) {
  await page.goto(`${BASE}/login`, { waitUntil: "networkidle" });
  await page.locator("input[type=email]").fill(account.email);
  await page.locator("input[autocomplete=current-password]").fill(account.password);
}

async function submitLogin(page) {
  await page.getByRole("button", { name: "Login" }).click();
  await page.waitForURL((url) => !url.pathname.endsWith("/login"), { timeout: 15000 });
  await page.waitForTimeout(800);
}

async function openPath(page, path) {
  await page.goto(`${BASE}${path}`, { waitUntil: "networkidle" });
  await page.waitForTimeout(600);
}

async function fillBySpan(page, label, value) {
  const field = page.locator("label.field", { has: page.locator("span", { hasText: label }) }).first();
  const input = field.locator("input, textarea, select").first();
  await input.fill(value);
}

const captures = [];

async function capture(page, id, title) {
  const file = await shot(page, id);
  captures.push({ id, title, file });
}

async function run() {
  ensureGuideUser();
  const browser = await chromium.launch({
    channel: "chrome",
    headless: true,
    args: ["--no-sandbox", "--disable-dev-shm-usage"],
  });
  const page = await browser.newPage({
    viewport: { width: 1440, height: 900 },
    deviceScaleFactor: 1,
  });

  try {
    await login(page, PLATFORM);
    await capture(page, "01-login", "Sign in");
    await submitLogin(page);
    await openPath(page, "/platform/tenants");
    await capture(page, "02-tenants", "Organizations (tenants)");
    await openPath(page, "/platform/tenants/new");
    await fillBySpan(page, "Organization name", "Example Education Group");
    await fillBySpan(page, "Email", "org@example.com");
    await capture(page, "03-create-tenant", "Create an organization");
    await openPath(page, "/platform/tenants/2/schools/new");
    await fillBySpan(page, "School name", "Tabata Primary School");
    await fillBySpan(page, "Email", "school@example.com");
    await capture(page, "04-add-school", "Add a school");
    await openPath(page, "/platform/schools");
    await capture(page, "05-schools", "Schools on the platform");

    await page.evaluate(() => {
      localStorage.clear();
    });
    await login(page, SCHOOL);
    await submitLogin(page);
    await openPath(page, "/dashboard");
    await capture(page, "06-dashboard", "School home");

    await openPath(page, "/admins");
    await page.getByRole("button", { name: "Add school admin" }).click();
    await fillBySpan(page, "Name", "Asha Mwanga");
    await fillBySpan(page, "Email", "asha.admin@example.com");
    await capture(page, "07-admins", "Add a school admin");

    await openPath(page, "/academics");
    await page.locator("form", { hasText: "New year" }).locator("input").nth(0).fill("2026/2027");
    await capture(page, "08-academics-year", "Academics: year, class, section, subject, department");

    await openPath(page, "/teachers");
    await page.getByRole("button", { name: "Add teacher" }).click();
    await fillBySpan(page, "Name", "Neema Kileo");
    await fillBySpan(page, "Employee ID", "T-104");
    await fillBySpan(page, "Email", "neema.kileo@example.com");
    const dept = page.locator("label.field", { hasText: "Department" }).locator("select");
    if (await dept.count()) {
      const options = await dept.locator("option").allTextContents();
      const science = options.find((o) => o.includes("Science"));
      if (science) await dept.selectOption({ label: science });
    }
    await capture(page, "09-teachers", "Add a teacher");

    await openPath(page, "/students");
    await page.getByRole("button", { name: "Admit student" }).click();
    await fillBySpan(page, "Name", "Baraka Juma");
    await fillBySpan(page, "Admission no", "ADM-2026-014");
    await fillBySpan(page, "Email", "baraka.juma@example.com");
    await capture(page, "10-students", "Admit a student");

    await openPath(page, "/parents");
    await page.getByRole("button", { name: "Add parent" }).click();
    await fillBySpan(page, "Name", "Fatma Juma");
    await fillBySpan(page, "Email", "fatma.juma@example.com");
    await capture(page, "11-parents", "Add a parent");

    await openPath(page, "/timetable");
    await capture(page, "12-timetable", "Timetable");
    await openPath(page, "/exams");
    await capture(page, "13-exams", "Exams");
    await openPath(page, "/grades");
    await capture(page, "14-grades", "Grades");
    await openPath(page, "/attendance");
    await capture(page, "15-attendance", "Attendance");
    await openPath(page, "/finance");
    await capture(page, "16-finance", "Finance");
    await openPath(page, "/notices");
    await page.locator("input").first().fill("Parents meeting on Friday");
    await capture(page, "17-notices", "Notices");
    await openPath(page, "/branding");
    await capture(page, "18-branding", "School branding");
  } finally {
    await browser.close();
    removeGuideUser();
  }
}

function img(id) {
  const item = captures.find((c) => c.id === id);
  if (!item || !existsSync(item.file)) return "";
  const b64 = readFileSync(item.file).toString("base64");
  return `<figure><img src="data:image/png;base64,${b64}" alt="${item.title}" /><figcaption>${item.title}</figcaption></figure>`;
}

function html() {
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <title>Halo School Management System — Setup Guide</title>
  <style>
    @page { size: A4; margin: 16mm 14mm 18mm; }
    * { box-sizing: border-box; }
    body {
      margin: 0;
      font-family: "Manrope", "Segoe UI", sans-serif;
      color: #102033;
      font-size: 11.5pt;
      line-height: 1.45;
    }
    h1, h2, h3 { font-family: "Fraunces", Georgia, serif; color: #06101f; }
    h1 { font-size: 28pt; margin: 0 0 8px; }
    h2 { font-size: 16pt; margin: 0 0 8px; page-break-after: avoid; }
    h3 { font-size: 13pt; margin: 16px 0 6px; }
    p { margin: 0 0 8px; }
    .cover {
      min-height: 240mm;
      padding: 28mm 8mm 0;
      page-break-after: always;
    }
    .kicker { letter-spacing: 0.14em; text-transform: uppercase; font-size: 10pt; color: #8a6a12; font-weight: 700; }
    .lead { font-size: 13pt; color: #3d5068; max-width: 140mm; }
    .meta { margin-top: 28mm; color: #5b6f88; font-size: 10pt; }
    .step { page-break-inside: avoid; margin: 0 0 16px; }
    .step-head { display: flex; gap: 10px; align-items: baseline; margin-bottom: 6px; }
    .num {
      flex: 0 0 28px; height: 28px; border-radius: 50%;
      background: #06101f; color: #f5c451; font-weight: 800;
      display: inline-flex; align-items: center; justify-content: center;
      font-size: 11pt;
    }
    ol.actions { margin: 6px 0 10px 22px; padding: 0; }
    ol.actions li { margin: 0 0 4px; }
    figure { margin: 8px 0 4px; page-break-inside: avoid; }
    figure img {
      width: 100%;
      border: 1px solid #d5deea;
      border-radius: 6px;
    }
    figcaption { font-size: 9pt; color: #5b6f88; margin-top: 4px; }
    .note { background: #f7f1dc; border-left: 4px solid #f5c451; padding: 8px 10px; margin: 8px 0 12px; }
    table { width: 100%; border-collapse: collapse; margin: 8px 0 14px; font-size: 10.5pt; }
    th, td { border: 1px solid #d5deea; padding: 6px 8px; text-align: left; vertical-align: top; }
    th { background: #06101f; color: #f4f7ff; }
    .toc a { color: #06101f; text-decoration: none; }
    .toc li { margin: 0 0 5px; }
    footer { color: #7a8aa0; font-size: 9pt; }
  </style>
</head>
<body>
  <section class="cover">
    <p class="kicker">Halo School Management System</p>
    <h1>School setup user guide</h1>
    <p class="lead">Create an organization, add a school, then set up teachers, classes, and students in the order the system requires.</p>
    <div class="meta">
      <p>For school and organization administrators</p>
      <p>Halo Campus · September 2026</p>
    </div>
  </section>

  <h2>What to create, and in what order</h2>
  <p>Each row needs the rows above it. Departments are created automatically when a school is added.</p>
  <table>
    <thead><tr><th>#</th><th>Create</th><th>Where</th><th>Needs</th></tr></thead>
    <tbody>
      <tr><td>1</td><td>Organization</td><td>Tenants</td><td>Platform admin</td></tr>
      <tr><td>2</td><td>School</td><td>Add school</td><td>Active organization</td></tr>
      <tr><td>3</td><td>School admin</td><td>Admins</td><td>Open school</td></tr>
      <tr><td>4</td><td>Academic year</td><td>Academics</td><td>School</td></tr>
      <tr><td>5</td><td>Classes</td><td>Academics</td><td>Year</td></tr>
      <tr><td>6</td><td>Teachers</td><td>Teachers</td><td>School (department optional)</td></tr>
      <tr><td>7</td><td>Sections</td><td>Academics</td><td>Class</td></tr>
      <tr><td>8</td><td>Subjects</td><td>Academics</td><td>School</td></tr>
      <tr><td>9</td><td>Students</td><td>Students</td><td>Year, class, section</td></tr>
      <tr><td>10</td><td>Parents</td><td>Parents</td><td>Student, if linking</td></tr>
      <tr><td>11</td><td>Allocations</td><td>Academics</td><td>Teacher, subject, class, year</td></tr>
    </tbody>
  </table>
  <p class="note">New passwords need 10+ characters with upper, lower, a digit, and a special character such as <code>HaloCampus1!</code>.</p>

  <div class="step">
    <div class="step-head"><span class="num">0</span><h2>Sign in</h2></div>
    <ol class="actions">
      <li>Open the Halo app (local development: <code>http://localhost:5173</code>).</li>
      <li>Enter your email and password, then click <strong>Login</strong>.</li>
    </ol>
    <p>Platform seed account: <code>halo.admin@halo-schools.net</code> / <code>ChangeMe123!</code>. Organization and school users use the emails created for them.</p>
    ${img("01-login")}
  </div>

  <div class="step">
    <div class="step-head"><span class="num">1</span><h2>Create the organization</h2></div>
    <ol class="actions">
      <li>Sign in as platform admin.</li>
      <li>Open <strong>Tenants</strong>.</li>
      <li>Click <strong>Create tenant</strong>.</li>
      <li>Enter the organization name (required). Email, phone, and country are optional.</li>
      <li>Click <strong>Create tenant</strong>.</li>
    </ol>
    <p>This creates the organization and its organization admin only. Schools are added next.</p>
    ${img("02-tenants")}
    ${img("03-create-tenant")}
  </div>

  <div class="step">
    <div class="step-head"><span class="num">2</span><h2>Add a school</h2></div>
    <ol class="actions">
      <li>From the tenant, click <strong>Add school</strong> (or use <strong>Schools</strong> on the platform).</li>
      <li>Enter the school name. Email, phone, and country are optional.</li>
      <li>Click <strong>Add school</strong>.</li>
    </ol>
    <p class="note">The organization must be <strong>Active</strong>. Halo then seeds the 13 standard departments (Languages, Mathematics, Science, and the rest).</p>
    ${img("04-add-school")}
    ${img("05-schools")}
  </div>

  <div class="step">
    <div class="step-head"><span class="num">3</span><h2>Open the school and add a school admin</h2></div>
    <ol class="actions">
      <li>As organization admin, open <strong>Schools</strong> and click the school to work in it.</li>
      <li>You land on the school home.</li>
      <li>Open <strong>Admins</strong> → <strong>Add school admin</strong>.</li>
      <li>Enter name, email, phone, and a strong password, then save.</li>
    </ol>
    <p>School admins run this school. They cannot create other organizations.</p>
    ${img("06-dashboard")}
    ${img("07-admins")}
  </div>

  <div class="step">
    <div class="step-head"><span class="num">4–5, 7–8, 11</span><h2>Configure academics</h2></div>
    <p>Open <strong>Academics</strong>. Work the cards from left to right.</p>
    <ol class="actions">
      <li><strong>New year</strong> — name (for example 2026/2027), start date, end date. Add year.</li>
      <li><strong>New class</strong> — choose the year, then name and code (Form 1 / F1). Add class.</li>
      <li><strong>New section</strong> — choose the class, then name (A). Add section.</li>
      <li><strong>New subject</strong> — name and code (Mathematics / MATH). Add subject.</li>
      <li>Departments are already listed. Add extras only if you need them.</li>
      <li>After teachers exist, use <strong>Allocate teacher</strong> (teacher + subject + class + year).</li>
    </ol>
    ${img("08-academics-year")}
  </div>

  <div class="step">
    <div class="step-head"><span class="num">6</span><h2>Add teachers</h2></div>
    <ol class="actions">
      <li>Open <strong>Teachers</strong> → <strong>Add teacher</strong>.</li>
      <li>Enter name, employee ID, email, and phone.</li>
      <li>Choose a <strong>Department</strong> from the dropdown (created in step 2).</li>
      <li>Set a strong password and click <strong>Save teacher</strong>.</li>
    </ol>
    ${img("09-teachers")}
  </div>

  <div class="step">
    <div class="step-head"><span class="num">9</span><h2>Admit students</h2></div>
    <ol class="actions">
      <li>Open <strong>Students</strong> → <strong>Admit student</strong>.</li>
      <li>Enter name, admission number, email, and phone.</li>
      <li>Choose year, class, and section.</li>
      <li>Set a password and save.</li>
    </ol>
    ${img("10-students")}
  </div>

  <div class="step">
    <div class="step-head"><span class="num">10</span><h2>Add parents and link them</h2></div>
    <ol class="actions">
      <li>Open <strong>Parents</strong> → <strong>Add parent</strong>.</li>
      <li>Enter name, email, phone, occupation, and password.</li>
      <li>Click <strong>Link student</strong> to connect a parent to a student and set the relationship.</li>
    </ol>
    ${img("11-parents")}
  </div>

  <h2>After the school is staffed</h2>
  <p>These can be done in any order once year, class, section, subject, teacher, and student exist.</p>
  <table>
    <thead><tr><th>Task</th><th>Where</th><th>What you do</th></tr></thead>
    <tbody>
      <tr><td>Weekly slots</td><td>Timetable</td><td>Pick a section, then add day, time, subject, teacher, room.</td></tr>
      <tr><td>Exams</td><td>Exams</td><td>Create an exam for a year and class, then add subject papers.</td></tr>
      <tr><td>Marks</td><td>Grades</td><td>Choose exam and subject, enter marks, save.</td></tr>
      <tr><td>Daily roll</td><td>Attendance</td><td>Choose section and date, mark present/absent, save.</td></tr>
      <tr><td>Fees</td><td>Finance</td><td>Add a fee structure, generate invoices, record payments.</td></tr>
      <tr><td>Announcements</td><td>Notices</td><td>Write a title and body, pick audience, publish.</td></tr>
      <tr><td>Look and feel</td><td>Branding</td><td>School name, colours, logo, timezone, currency.</td></tr>
    </tbody>
  </table>
  ${img("12-timetable")}
  ${img("13-exams")}
  ${img("14-grades")}
  ${img("15-attendance")}
  ${img("16-finance")}
  ${img("17-notices")}
  ${img("18-branding")}

  <h2>Minimum path</h2>
  <p>To get a usable school quickly: <strong>organization → school → year → class → teacher → section → subject → student</strong>.</p>
  <footer>Halo School Management System · Setup user guide · Screenshots from the Halo web app</footer>
</body>
</html>`;
}

async function printPdf() {
  const browser = await chromium.launch({
    channel: "chrome",
    headless: true,
    args: ["--no-sandbox", "--disable-dev-shm-usage"],
  });
  const page = await browser.newPage();
  const doc = join(ROOT, "Halo-School-Setup-Guide.html");
  const pdf = join(ROOT, "Halo-School-Setup-Guide.pdf");
  writeFileSync(doc, html());
  await page.goto(`file://${doc}`, { waitUntil: "load" });
  await page.pdf({
    path: pdf,
    format: "A4",
    printBackground: true,
    displayHeaderFooter: true,
    headerTemplate: `<div></div>`,
    footerTemplate: `<div style="font-size:9px;color:#7a8aa0;width:100%;padding:0 14mm;display:flex;justify-content:space-between;"><span>Halo School Management System</span><span>Page <span class="pageNumber"></span> of <span class="totalPages"></span></span></div>`,
    margin: { top: "14mm", bottom: "16mm", left: "12mm", right: "12mm" },
  });
  await browser.close();
  console.log(pdf);
}

await run();
await printPdf();
