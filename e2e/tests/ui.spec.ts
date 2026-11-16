import { expect, test } from '../support/fixtures'

/**
 * The reader's paths through the site. Runs twice (see playwright.config.ts): against the frontend
 * directly and through Apache -> Varnish.
 */
test.describe('reading the site', () => {
  test('the start page arrives fully rendered from the server', async ({ request }) => {
    // No browser, no JavaScript: what the server sends must already contain the articles.
    const response = await request.get('/')
    const html = await response.text()

    expect(response.status()).toBe(200)
    expect(html.match(/data-test="article-title"/g) ?? []).toHaveLength(8)
    expect(html).toContain('EU einigt sich auf neues Klimapaket')
  })

  test('the start page lists all articles, newest first', async ({ page }) => {
    await page.goto('/')

    const titles = page.locator('[data-test="article-title"]')
    await expect(titles).toHaveCount(8)
    await expect(titles.first()).toHaveText('EU einigt sich auf neues Klimapaket')
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Aktuelle Meldungen')
  })

  test('a section filter narrows the list without a full page load', async ({ page }) => {
    await page.goto('/')
    await page.evaluate(() => { (window as unknown as { __sameDocument: boolean }).__sameDocument = true })

    await page.getByRole('navigation', { name: 'Ressorts' }).getByRole('link', { name: 'Politik' }).click()

    await expect(page).toHaveURL(/\/\?category=politik$/)
    await expect(page.locator('[data-test="article-title"]')).toHaveCount(2)
    await expect(page.locator('[data-test="article-category"]')).toHaveText(['Politik', 'Politik'])
    // Still the same document: the list was fetched by the hydrated app, not by a reload.
    expect(await page.evaluate(() => (window as unknown as { __sameDocument?: boolean }).__sameDocument)).toBe(true)
  })

  test('an article opens with breadcrumb, author and body', async ({ page }) => {
    await page.goto('/')

    await page.getByRole('link', { name: 'EU einigt sich auf neues Klimapaket' }).first().click()

    await expect(page).toHaveURL(/\/article\/1$/)
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('EU einigt sich auf neues Klimapaket')
    const breadcrumb = page.getByRole('navigation', { name: 'Brotkrumen-Navigation' })
    await expect(breadcrumb.getByRole('link', { name: 'Politik' })).toHaveAttribute('href', '/?category=politik')
    await expect(page.locator('.article-detail__meta')).toContainText('Lena Hoffmann')
    await expect(page.locator('.article-detail__body')).toContainText('umfassendes Klimapaket')
    await expect(page).toHaveTitle('EU einigt sich auf neues Klimapaket · NewsCore')
  })

  test('an article opened by its address hydrates cleanly and shows the time in the site\'s zone', async ({ page }) => {
    await page.goto('/article/2')

    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Inflation sinkt den dritten Monat in Folge')
    // Published 11:40 UTC = 13:40 in Berlin, whatever zone the server or the browser is in.
    await expect(page.locator('.article-detail__meta time')).toHaveText('17. Juni 2026 um 13:40')
    // Hydrated: a client-side navigation from here works.
    await page.getByRole('navigation', { name: 'Brotkrumen-Navigation' }).getByRole('link', { name: 'Wirtschaft' }).click()
    await expect(page).toHaveURL(/\/\?category=wirtschaft$/)
    await expect(page.locator('[data-test="article-title"]')).toHaveCount(2)
  })

  test('the search finds articles by a word in title, teaser or tags', async ({ page }) => {
    await page.goto('/')

    await page.getByRole('searchbox', { name: 'Artikel suchen' }).fill('klima')
    await page.getByRole('button', { name: 'Suchen' }).click()

    await expect(page).toHaveURL(/\/search\?q=klima$/)
    await expect(page.locator('.search-summary')).toContainText('2 Treffer')
    const found = await page.locator('[data-test="article-title"]').allTextContents()
    expect(found.map((title) => title.trim()).sort()).toEqual([
      'EU einigt sich auf neues Klimapaket',
      'Startup-Finanzierungen ziehen wieder an',
    ])
  })

  test('a search without matches says so', async ({ page }) => {
    await page.goto('/search?q=supercalifragilistic')

    await expect(page.locator('.search-summary')).toContainText('0 Treffer')
    await expect(page.getByText('Keine Treffer.')).toBeVisible()
  })

  test('server-rendered pages carry a script policy without unsafe-inline', async ({ request }) => {
    const response = await request.get('/article/1')
    const policy = response.headers()['content-security-policy'] ?? ''
    const scriptSrc = policy.split('; ').find((directive) => directive.startsWith('script-src')) ?? ''

    expect(scriptSrc).toMatch(/^script-src 'self'( 'sha256-[A-Za-z0-9+/=]+')+$/)
    expect(policy).toContain("connect-src 'self'")
  })
})

test.describe('an article that does not exist', () => {
  // The browser reports the 404 of the document itself as a console error; that one is expected.
  test.use({ allowedConsoleErrors: [/status of 404/] })

  test('answers with HTTP 404 and an error page, not with an empty article', async ({ page }) => {
    const response = await page.goto('/article/does-not-exist')

    expect(response?.status()).toBe(404)
    await expect(page.locator('[data-test="error-page"]')).toContainText('Seite nicht gefunden')

    await page.getByRole('link', { name: 'Zur Startseite' }).click()
    await expect(page).toHaveURL(/\/$/)
    await expect(page.locator('[data-test="article-title"]')).toHaveCount(8)
  })
})
