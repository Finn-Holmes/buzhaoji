// Run with: node --test app/src/test/js/academic-weekday.test.cjs
// Execute the actual WebView extraction scripts against small DOM-shaped fixtures.
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const { test } = require('node:test');
const source = fs.readFileSync(path.join(__dirname,
  '../../main/java/com/kexu/app/importer/AcademicWebAdapter.kt'), 'utf8');
function script(name) {
  const match = source.match(new RegExp('val ' + name + ': String = """([\\s\\S]*?)"""\\.trimIndent\\(\\)'));
  assert.ok(match, name);
  return match[1].replace('$timetableGridScript', () => script('timetableGridScript'));
}
function font(text, title = '') {
  return { textContent: text, getAttribute: key => key === 'title' ? title : null };
}
function cell(text = '', colSpan = 1, rowSpan = 1, course = false) {
  const fonts = [font(text), font('测试教师', '教师'), font('1-16(周)[1-2节]', '周次(节次)'), font('A101', '教室')];
  const detail = {
    textContent: text, innerText: text + '\n1-16周\n1-2节',
    getAttribute: () => null, querySelectorAll: selector => selector === 'font' ? fonts : [],
  };
  return {
    innerText: text, colSpan, rowSpan, children: [], getAttribute: () => null,
    querySelectorAll: () => course ? [detail] : [],
  };
}
function table(rowCells) {
  const group = {};
  return { rows: rowCells.map(cells => ({ cells, parentElement: group })) };
}
function extract(name, timetable) {
  const document = {
    title: '学期理论课表', location: { pathname: '/xskb/xskb_list.do' },
    getElementById: id => id === 'timetable' ? timetable : null,
    querySelectorAll: selector => selector === 'table' ? [timetable] : [],
  };
  return JSON.parse(vm.runInNewContext(script(name), { document }));
}
const adapters = ['qiangzhiExtractionScript', 'genericTableExtractionScript'];
for (const adapter of adapters) {
  test(adapter + ': Monday-first short headers preserve all seven days', () => {
    const headers = ['一', '二', '三', '四', '五', '六', '日'];
    const result = extract(adapter, table([
      [cell('节次'), ...headers.map(name => cell(name))],
      [cell('第1-2节'), ...headers.map((_, i) => cell('课程' + (i + 1), 1, 1, true))],
    ]));
    assert.deepEqual(result.courses.map(course => [course.name, course.day]),
      headers.map((_, i) => ['课程' + (i + 1), i + 1]));
  });
  test(adapter + ': Sunday-first headers use actual labels', () => {
    const days = [7, 1, 2, 3, 4, 5, 6];
    const result = extract(adapter, table([
      days.map(day => cell('星期' + day)),
      days.map(day => cell('课程' + day, 1, 1, true)),
    ]));
    assert.deepEqual(result.courses.map(course => course.day), days);
  });
  test(adapter + ': merged leading columns and rowspans keep weekday alignment', () => {
    const result = extract(adapter, table([
      [cell('时间', 2), ...['一', '二', '三', '四', '五', '六', '日'].map(day => cell('周' + day))],
      [cell('上午', 1, 2), cell('第1-2节'), cell('高等数学', 1, 2, true),
        cell(''), cell('大学物理', 1, 1, true), cell(''), cell(''), cell(''), cell('')],
      [cell('第3-4节'), cell('大学英语', 1, 1, true), cell(''), cell(''), cell(''), cell(''), cell('')],
    ]));
    assert.deepEqual(result.courses.map(course => [course.name, course.day]),
      [['高等数学', 1], ['大学物理', 3], ['大学英语', 2]]);
  });
  test(adapter + ': missing headers cannot silently invent a Sunday-first week', () => {
    const result = extract(adapter, table([
      [cell('学期理论课表', 7)],
      Array.from({ length: 7 }, (_, i) => cell('课程' + i, 1, 1, true)),
    ]));
    assert.ok(result.error);
    assert.equal(result.courses, undefined);
  });
  test(adapter + ': a cross-weekday merged cell is not assigned to the wrong day', () => {
    const result = extract(adapter, table([
      ['一', '二', '三', '四', '五', '六', '日'].map(day => cell('周' + day)),
      [cell('歧义课程', 2, 1, true), cell('明确课程', 1, 1, true), cell(''), cell(''), cell(''), cell('')],
    ]));
    assert.deepEqual(result.courses.map(course => [course.name, course.day]), [['明确课程', 3]]);
  });
}
