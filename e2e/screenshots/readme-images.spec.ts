import path from 'node:path'
import { expect, test } from '@playwright/test'

const images = path.resolve(__dirname, '../../docs/images')

test('README images: start page, article, search', async ({ page }) => {
  await page.goto('/')
  await expect(page.locator('[data-test="article-title"]')).toHaveCount(8)
  await page.waitForLoadState('networkidle')
  await page.screenshot({ path: path.join(images, 'screenshot-home.png') })
  await page.waitForTimeout(1200)

  await page.getByRole('navigation', { name: 'Ressorts' }).getByRole('link', { name: 'Politik' }).click()
  await expect(page.locator('[data-test="article-title"]')).toHaveCount(2)
  await page.waitForTimeout(1200)

  await page.getByRole('link', { name: 'EU einigt sich auf neues Klimapaket' }).first().click()
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('EU einigt sich auf neues Klimapaket')
  await page.waitForLoadState('networkidle')
  await page.screenshot({ path: path.join(images, 'screenshot-article.png') })
  await page.waitForTimeout(1500)

  await page.getByRole('searchbox', { name: 'Artikel suchen' }).pressSequentially('klima', { delay: 120 })
  await page.getByRole('button', { name: 'Suchen' }).click()
  await expect(page.locator('.search-summary')).toContainText('2 Treffer')
  await page.waitForLoadState('networkidle')
  await page.screenshot({ path: path.join(images, 'screenshot-search.png') })
  await page.waitForTimeout(1500)
})
