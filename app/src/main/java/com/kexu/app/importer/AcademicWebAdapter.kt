package com.kexu.app.importer

import com.kexu.app.data.model.WeekPattern
import java.time.DayOfWeek
import org.json.JSONArray
import org.json.JSONObject

object AcademicWebAdapter {
    data class ExtractionAdapter(
        val id: String,
        val label: String,
        val script: String,
    )

    fun adaptersFor(system: EducationSystem): List<ExtractionAdapter> {
        val dedicated = listOf(
            ExtractionAdapter("qiangzhi", "强智课表", qiangzhiExtractionScript),
            ExtractionAdapter("zhengfang", "正方课表", zhengfangExtractionScript),
            ExtractionAdapter("urp", "URP 课表", urpExtractionScript),
            ExtractionAdapter("qingguo", "青果课表", qingguoExtractionScript),
        )
        val generic = ExtractionAdapter("generic", "通用表格", genericTableExtractionScript)
        return when (system) {
            EducationSystem.AUTO -> dedicated + generic
            EducationSystem.QIANGZHI -> listOf(dedicated[0], generic)
            EducationSystem.ZHENGFANG -> listOf(dedicated[1], generic)
            EducationSystem.URP -> listOf(dedicated[2], generic)
            EducationSystem.QINGGUO -> listOf(dedicated[3], generic)
            EducationSystem.UNKNOWN -> listOf(generic)
        }
    }

    fun scriptsFor(system: EducationSystem): List<String> = adaptersFor(system).map(ExtractionAdapter::script)

    // Resolve visual table coordinates before associating courses with weekday headers.
    // DOM cell indices omit columns occupied by rowspan/colspan cells.
    private val timetableGridScript: String = """
          const weekdayOf = value => {
            const text = (value || '').replace(/\s/g, '').toLowerCase();
            const chinese = text.match(/^(?:(?:星期|周|礼拜)([一二三四五六日天1-7])|([一二三四五六日天]))(?:$|[（(\d])/);
            if (chinese) {
              const token = chinese[1] || chinese[2];
              return '日天'.includes(token) ? 7 : ('一二三四五六'.indexOf(token) + 1 || Number(token));
            }
            const english = ['monday','tuesday','wednesday','thursday','friday','saturday','sunday'];
            const index = english.findIndex(name => text === name || text === name.slice(0, 3));
            return index < 0 ? 0 : index + 1;
          };
          const timetableGrid = table => {
            const rows = Array.from(table.rows);
            const grid = rows.map(() => []);
            rows.forEach((row, rowIndex) => {
              let column = 0;
              for (const cell of Array.from(row.cells)) {
                while (grid[rowIndex][column]) column++;
                const width = Math.max(1, cell.colSpan || 1);
                const remaining = rows.slice(rowIndex).filter(item => item.parentElement === row.parentElement).length;
                const height = cell.rowSpan === 0 ? remaining : Math.max(1, cell.rowSpan || 1);
                for (let r = rowIndex; r < Math.min(rows.length, rowIndex + height); r++) {
                  for (let c = column; c < column + width; c++) grid[r][c] = cell;
                }
                column += width;
              }
            });
            const headerIndex = grid.findIndex(cells =>
              new Set(cells.map(cell => weekdayOf(cell?.innerText)).filter(Boolean)).size >= 5
            );
            const columns = headerIndex < 0 ? [] : grid[headerIndex]
              .map((cell, column) => [column, weekdayOf(cell?.innerText)])
              .filter(pair => pair[1]);
            // A cell spanning two different days is ambiguous; never assign it twice.
            const dayCells = cells => {
              const byCell = new Map();
              for (const [column, day] of columns) {
                const cell = cells[column];
                if (cell) byCell.set(cell, byCell.has(cell) && byCell.get(cell) !== day ? 0 : day);
              }
              return [...byCell].filter(pair => pair[1]);
            };
            return { rows, grid, headerIndex, columns, dayCells };
          };
    """.trimIndent()

    /**
     * Runs only after the user taps Confirm Import. It reads a Strong/Qiangzhi timetable
     * from the current document or a same-origin iframe and returns normalized JSON.
     */
    val qiangzhiExtractionScript: String = """
        (() => {
          const documents = [];
          const isFrameVisible = frame => {
            try {
              const style = frame.ownerDocument.defaultView.getComputedStyle(frame);
              const rect = frame.getBoundingClientRect();
              return style.display !== 'none' && style.visibility !== 'hidden' &&
                Number(style.opacity || '1') !== 0 && rect.width > 0 && rect.height > 0;
            } catch (_) {
              return false;
            }
          };
          const collectDocs = (doc, depth, visible) => {
            if (!doc || depth > 5 || documents.some(item => item.doc === doc)) return;
            let path = '';
            try { path = doc.location.pathname || ''; } catch (_) {}
            documents.push({ doc, depth, visible, path });
            for (const frame of doc.querySelectorAll('iframe')) {
              try { collectDocs(frame.contentDocument, depth + 1, visible && isFrameVisible(frame)); } catch (_) {}
            }
          };
          collectDocs(document, 0, true);

          $timetableGridScript

          const parseTable = timetable => {
            const courses = [];
            const seen = new Set();
            const layout = timetableGrid(timetable);
            if (layout.headerIndex < 0) return courses;
            const processedCells = new Set();
            for (const cells of layout.grid.slice(layout.headerIndex + 1)) {
              layout.dayCells(cells).forEach(([cell, day]) => {
                if (processedCells.has(cell)) return;
                processedCells.add(cell);
                const allDetails = Array.from(cell.querySelectorAll('.kbcontent, .kbcontent1'))
                  .filter(detail => (detail.textContent || '').trim());
                const visibleDetails = allDetails.filter(detail => {
                  const style = (detail.getAttribute('style') || '').replace(/\s/g, '').toLowerCase();
                  return !style.includes('display:none');
                });
                // Strong renders a compact hidden copy and a detailed visible copy for every
                // cell. Reading both creates duplicates; reading only the first one loses the
                // teacher and section range. Prefer the visible detailed copy and keep the
                // compact copy only as a compatibility fallback for older installations.
                for (const detail of (visibleDetails.length ? visibleDetails : allDetails)) {
                  const fonts = Array.from(detail.querySelectorAll('font'));
                  for (let index = 0; index < fonts.length; index++) {
                    const weekFont = fonts[index];
                    if ((weekFont.getAttribute('title') || '') !== '周次(节次)') continue;
                    const weekInfo = (weekFont.textContent || '').trim();
                    const section = weekInfo.match(/\[\s*(\d{1,2}(?:\s*-\s*\d{1,2})*)\s*节\]/);
                    if (!section) continue;

                    let teacherIndex = -1;
                    for (let previous = index - 1; previous >= 0; previous--) {
                      const title = fonts[previous].getAttribute('title') || '';
                      if (title === '周次(节次)') break;
                      if (title === '教师') {
                        teacherIndex = previous;
                        break;
                      }
                    }
                    const teacher = teacherIndex >= 0 ? (fonts[teacherIndex].textContent || '').trim() : '';
                    let name = '';
                    const nameEnd = teacherIndex >= 0 ? teacherIndex : index;
                    for (let previous = nameEnd - 1; previous >= 0; previous--) {
                      const title = fonts[previous].getAttribute('title') || '';
                      if (title === '周次(节次)') break;
                      const value = (fonts[previous].textContent || '').trim();
                      if (value && !title && !fonts[previous].getAttribute('name')) {
                        name = value;
                        break;
                      }
                    }
                    let position = '';
                    for (let next = index + 1; next < fonts.length; next++) {
                      const title = fonts[next].getAttribute('title') || '';
                      if (title === '周次(节次)') break;
                      if (title === '教室') {
                        position = (fonts[next].textContent || '').trim();
                        break;
                      }
                    }

                    const sectionNumbers = section[1].split('-').map(Number).filter(Number.isFinite);
                    if (!name || !sectionNumbers.length) continue;
                    const oddWeeksOnly = weekInfo.includes('单周');
                    const evenWeeksOnly = weekInfo.includes('双周');
                    const weeks = [];
                    const rawWeeks = weekInfo.split('(')[0];
                    for (const part of rawWeeks.split(/[,，、]/)) {
                      const range = part.trim().split(/[-~至]/).map(Number);
                      if (!Number.isFinite(range[0])) continue;
                      const end = Number.isFinite(range[1]) ? range[1] : range[0];
                      for (let week = range[0]; week <= end && week <= 63; week++) {
                        if ((!oddWeeksOnly || week % 2 === 1) && (!evenWeeksOnly || week % 2 === 0)) weeks.push(week);
                      }
                    }
                    const uniqueWeeks = [...new Set(weeks)].sort((left, right) => left - right);
                    if (!uniqueWeeks.length) continue;
                    const startSection = sectionNumbers[0];
                    const endSection = sectionNumbers[sectionNumbers.length - 1];
                    const key = [name, teacher, position, day, uniqueWeeks.join(','), startSection, endSection].join('|');
                    if (seen.has(key)) continue;
                    seen.add(key);
                    courses.push({ name, teacher, position, day, weeks: uniqueWeeks, startSection, endSection });
                  }
                }
              });
            }
            return courses;
          };

          const candidates = documents.flatMap(entry => {
            const timetable = entry.doc.getElementById('timetable');
            if (!timetable) return [];
            const courses = parseTable(timetable);
            const termSelector = entry.doc.getElementById('xnxq01id');
            const weekSelector = entry.doc.getElementById('zc');
            const selectedWeek = weekSelector && weekSelector.options
              ? (weekSelector.options[weekSelector.selectedIndex]?.textContent || '').trim()
              : '';
            const allWeeks = !!weekSelector && (weekSelector.value === '' || selectedWeek.includes('全部'));
            const title = (entry.doc.title || '').trim();
            const theoryPage = title.includes('学期理论课表') || /\/xskb\/xskb_list\.do$/i.test(entry.path);
            const maxWeek = courses.reduce((maximum, course) =>
              Math.max(maximum, ...(course.weeks || [0])), 0);
            const uniqueNames = new Set(courses.map(course => course.name)).size;
            const score = (theoryPage ? 10000000 : 0) +
              (termSelector ? 1000000 : 0) +
              (allWeeks ? 500000 : 0) +
              (entry.visible ? 100000 : 0) +
              courses.length * 100 + maxWeek;
            return [{ ...entry, courses, title, theoryPage, allWeeks, selectedWeek, maxWeek, uniqueNames, score }];
          });
          if (!candidates.length) {
            return JSON.stringify({
              adapter: 'qiangzhi-candidate-ranking-v3',
              error: '当前页面没有检测到强智课表，请先打开学期理论课表',
              candidates: [],
            });
          }
          candidates.sort((left, right) => right.score - left.score);
          const selected = candidates[0];
          const diagnostics = candidates.slice(0, 10).map((candidate, index) => ({
            index,
            path: candidate.path,
            title: candidate.title.slice(0, 60),
            visible: candidate.visible,
            theoryPage: candidate.theoryPage,
            allWeeks: candidate.allWeeks,
            selectedWeek: candidate.selectedWeek,
            arrangements: candidate.courses.length,
            uniqueNames: candidate.uniqueNames,
            maxWeek: candidate.maxWeek,
            score: candidate.score,
          }));
          return JSON.stringify(selected.courses.length
            ? {
                adapter: 'qiangzhi-candidate-ranking-v3',
                courses: selected.courses,
                selectedCandidate: 0,
                candidates: diagnostics,
              }
            : {
                adapter: 'qiangzhi-candidate-ranking-v3',
                error: '检测到了强智课表，但无法确认星期列或完整的周次和节次信息，请展开完整课表后重试',
                selectedCandidate: 0,
                candidates: diagnostics,
              });
        })()
    """.trimIndent()

    val zhengfangExtractionScript: String = """
        (() => {
          const docs = [document];
          for (let depth = 0; depth < 4; depth++) for (const doc of [...docs]) for (const frame of doc.querySelectorAll('iframe')) {
            try { if (frame.contentDocument && !docs.includes(frame.contentDocument)) docs.push(frame.contentDocument); } catch (_) {}
          }
          const doc = docs.find(item => item.querySelector('#kbgrid_table_0,[id^="kbgrid_table"]'));
          const table = doc && doc.querySelector('#kbgrid_table_0,[id^="kbgrid_table"]');
          if (!table) return JSON.stringify({ error: '未检测到正方教务网格课表' });
          const expand = value => {
            const result = [];
            for (const raw of (value || '').replace(/第|周/g, '').split(/[,，;；]/)) {
              const odd = raw.includes('单'), even = raw.includes('双');
              const bounds = raw.replace(/[单双]/g, '').trim().split(/[-~至]/).map(Number);
              if (!Number.isFinite(bounds[0])) continue;
              const end = Number.isFinite(bounds[1]) ? bounds[1] : bounds[0];
              for (let week = bounds[0]; week <= end && week <= 63; week++) if ((!odd || week % 2) && (!even || week % 2 === 0)) result.push(week);
            }
            return [...new Set(result)];
          };
          const courses = [];
          for (const cell of table.querySelectorAll('td')) {
            const day = Number((cell.id || '').split('-')[0]);
            if (day < 1 || day > 7) continue;
            for (const block of cell.querySelectorAll('.timetable_con,.kbcontent,.kbcontent1')) {
              const name = (block.querySelector('.title font,.title')?.textContent || '').replace(/[●★○]/g, '').trim();
              const paragraphs = Array.from(block.querySelectorAll('p')).map(item => item.textContent.trim());
              const joined = paragraphs.join('\n');
              const section = joined.match(/\((\d{1,2})\s*[-~至]\s*(\d{1,2})节\)/) || joined.match(/第?(\d{1,2})\s*[-~至]\s*(\d{1,2})节/);
              const weekText = section ? joined.slice((section.index || 0) + section[0].length) : joined;
              const weeks = expand(weekText);
              const position = (paragraphs[1] || '').replace(/^上课地点\s*[:：]?/, '').trim().split(/\s+/).pop() || '';
              const teacher = (paragraphs[2] || '').replace(/^教师\s*[:：]?/, '').trim();
              if (name && section && weeks.length) courses.push({ name, teacher, position, day, weeks, startSection: Number(section[1]), endSection: Number(section[2]) });
            }
          }
          return JSON.stringify(courses.length ? { adapter: 'zhengfang-grid-v1', courses } : { error: '正方课表中没有识别到课程，请切换到网格课表' });
        })()
    """.trimIndent()

    val urpExtractionScript: String = """
        (() => {
          const docs = [document];
          for (let depth = 0; depth < 4; depth++) for (const doc of [...docs]) for (const frame of doc.querySelectorAll('iframe')) {
            try { if (frame.contentDocument && !docs.includes(frame.contentDocument)) docs.push(frame.contentDocument); } catch (_) {}
          }
          const doc = docs.find(item => item.querySelector('.class_div'));
          if (!doc) return JSON.stringify({ error: '未检测到 URP 课表课程块' });
          const weeksOf = value => {
            const result = [];
            for (const raw of (value || '').replace(/第|周/g, '').split(/[,，;；]/)) {
              const odd = raw.includes('单'), even = raw.includes('双');
              const bounds = raw.replace(/[单双]/g, '').trim().split(/[-~至]/).map(Number);
              if (!Number.isFinite(bounds[0])) continue;
              const end = Number.isFinite(bounds[1]) ? bounds[1] : bounds[0];
              for (let week = bounds[0]; week <= end && week <= 63; week++) if ((!odd || week % 2) && (!even || week % 2 === 0)) result.push(week);
            }
            return [...new Set(result)];
          };
          const courses = [];
          for (const cell of doc.querySelectorAll('td[id*="_"]')) {
            const parts = cell.id.split('_');
            const day = Number(parts[0]);
            if (parts.length !== 2 || day < 1 || day > 7) continue;
            for (const block of cell.querySelectorAll('.class_div')) {
              const p = Array.from(block.querySelectorAll('p')).map(item => item.textContent.trim());
              if (p.length < 5) continue;
              const section = p[4].match(/(\d{1,2})(?:\s*[-~至]\s*(\d{1,2}))?节?/);
              const weeks = weeksOf(p[3]);
              if (p[0] && section && weeks.length) courses.push({
                name: p[0], teacher: (p[2] || '').replace(/\*/g, ' ').trim(), position: p[5] || '', day, weeks,
                startSection: Number(section[1]), endSection: Number(section[2] || section[1])
              });
            }
          }
          const timeSlots = Array.from(doc.querySelectorAll('th[id^="0_"]')).map(th => {
            const number = Number(th.id.split('_')[1]);
            const time = th.textContent.match(/\((\d{1,2}:\d{2})\s*[-~至]\s*(\d{1,2}:\d{2})\)/);
            return time && number ? { number, startTime: time[1], endTime: time[2] } : null;
          }).filter(Boolean);
          return JSON.stringify(courses.length ? { adapter: 'urp-grid-v1', courses, timeSlots } : { error: 'URP 课表中没有识别到课程' });
        })()
    """.trimIndent()

    val qingguoExtractionScript: String = """
        (() => {
          const docs = [document];
          for (let depth = 0; depth < 4; depth++) for (const doc of [...docs]) for (const frame of doc.querySelectorAll('iframe')) {
            try { if (frame.contentDocument && !docs.includes(frame.contentDocument)) docs.push(frame.contentDocument); } catch (_) {}
          }
          const table = docs.flatMap(doc => Array.from(doc.querySelectorAll('table'))).find(item => item.innerText.includes('星期一') && /\d+\s*[-~]\s*\d+\]/.test(item.innerText));
          if (!table) return JSON.stringify({ error: '未检测到青果教务课表' });
          const courses = [];
          for (const row of table.rows) {
            const cells = Array.from(row.cells);
            if (cells.length < 7) continue;
            cells.slice(-7).forEach((cell, index) => {
              const lines = cell.innerText.split('\n').map(value => value.trim()).filter(Boolean);
              lines.forEach((line, lineIndex) => {
                const match = line.match(/([\d,，-]+)\s*\[(\d{1,2})\s*[-~至]\s*(\d{1,2})\]/);
                if (!match) return;
                const weeks = [];
                for (const raw of match[1].replace(/，/g, ',').split(',')) {
                  const bounds = raw.split('-').map(Number);
                  const end = Number.isFinite(bounds[1]) ? bounds[1] : bounds[0];
                  for (let week = bounds[0]; week <= end && week <= 63; week++) weeks.push(week);
                }
                const name = lines[Math.max(0, lineIndex - 2)] || lines[Math.max(0, lineIndex - 1)] || '';
                const teacher = lineIndex > 0 && !lines[lineIndex - 1].includes('[') ? lines[lineIndex - 1] : '';
                const position = lines[lineIndex + 1] || '';
                if (name && weeks.length) courses.push({ name: name.replace(/\s/g, ''), teacher: teacher.replace(/\s/g, ''), position: position.replace(/\s/g, ''), day: index + 1, weeks: [...new Set(weeks)], startSection: Number(match[2]), endSection: Number(match[3]) });
              });
            });
          }
          return JSON.stringify(courses.length ? { adapter: 'qingguo-table-v1', courses } : { error: '青果课表中没有识别到课程' });
        })()
    """.trimIndent()

    /**
     * Conservative fallback for timetable pages rendered as an HTML table. It deliberately
     * requires weekday headers and week/section text, so an unrelated table is not imported.
     * The user still reviews every result in the native preview before anything is stored.
     */
    val genericTableExtractionScript: String = """
        (() => {
          const docs = [];
          const collectDocs = (doc, depth) => {
            if (!doc || depth > 4 || docs.includes(doc)) return;
            docs.push(doc);
            for (const frame of doc.querySelectorAll('iframe')) {
              try { collectDocs(frame.contentDocument, depth + 1); } catch (_) {}
            }
          };
          collectDocs(document, 0);
          $timetableGridScript
          const clean = value => (value || '').replace(/\u00a0/g, ' ').replace(/[ \t]+/g, ' ').trim();
          const parseWeeks = value => {
            const result = [];
            const text = clean(value).replace(/第/g, '');
            const matches = text.matchAll(/(\d+(?:\s*[-~至]\s*\d+)?(?:\s*[,，、]\s*\d+(?:\s*[-~至]\s*\d+)?)*)\s*周/g);
            for (const match of matches) {
              for (const part of match[1].split(/[,，、]/)) {
                const bounds = part.trim().split(/[-~至]/).map(Number);
                if (!Number.isFinite(bounds[0])) continue;
                const end = Number.isFinite(bounds[1]) ? bounds[1] : bounds[0];
                for (let week = bounds[0]; week <= end && week <= 63; week++) result.push(week);
              }
            }
            return [...new Set(result)].sort((a, b) => a - b);
          };
          const parseSections = value => {
            const match = clean(value).match(/(?:第|\[)?\s*(\d{1,2})\s*(?:[-~至]\s*(\d{1,2}))?\s*节/);
            if (!match) return null;
            return [Number(match[1]), Number(match[2] || match[1])];
          };
          const field = (text, labels) => {
            for (const label of labels) {
              const match = text.match(new RegExp(label + '\\s*[:：]?\\s*([^\\n]+)'));
              if (match) return clean(match[1]);
            }
            return '';
          };
          const courses = [];
          const seen = new Set();
          const timeSlotMap = new Map();
          let matchedTable = false;
          for (const doc of docs) {
            for (const element of doc.querySelectorAll('tr,li,[data-jc],[data-section],.festival,.section,.period')) {
              const text = clean(element.innerText);
              if (!text || text.length > 100) continue;
              const sectionMatch = text.match(/(?:第\s*)?(\d{1,2})\s*节/);
              const timeMatch = text.match(/(\d{1,2}:\d{2})\s*[-~至—–]\s*(\d{1,2}:\d{2})/);
              if (sectionMatch && timeMatch) {
                const number = Number(sectionMatch[1]);
                if (number > 0 && number <= 30 && !timeSlotMap.has(number)) {
                  timeSlotMap.set(number, { number, startTime: timeMatch[1], endTime: timeMatch[2] });
                }
              }
            }
            for (const table of doc.querySelectorAll('table')) {
              const layout = timetableGrid(table);
              const { headerIndex, columns } = layout;
              if (headerIndex < 0) continue;
              matchedTable = true;
              const processedCells = new Set();
              layout.grid.slice(headerIndex + 1).forEach((cells, rowOffset) => {
                const rowSections = parseSections(cells.slice(0, columns[0][0]).map(cell => cell?.innerText || '').join(' ')) || [rowOffset + 1, rowOffset + 1];
                for (const [cell, day] of layout.dayCells(cells)) {
                  if (processedCells.has(cell)) continue;
                  processedCells.add(cell);
                  let blocks = Array.from(cell.querySelectorAll('.timetable_con,.kbcontent,.kbcontent1,[class*="course"],[class*="Course"],[class*="kcb"],[class*="kbcontent"]'));
                  if (!blocks.length) blocks = Array.from(cell.children).filter(node => clean(node.innerText));
                  if (!blocks.length && clean(cell.innerText)) blocks = [cell];
                  for (const block of blocks) {
                    const text = clean(block.innerText).replace(/\s*\n\s*/g, '\n');
                    const weeks = parseWeeks(text);
                    const sections = parseSections(text) || rowSections;
                    if (!text || !weeks.length || !sections) continue;
                    const lines = text.split('\n').map(clean).filter(Boolean);
                    const name = clean(block.getAttribute('data-course-name')) || lines.find(line =>
                      !/^(教师|老师|教室|地点|上课地点|周次|时间|星期|周[一二三四五六日天])[:：]?/.test(line) &&
                      !/^第?\d+(?:[-~至]\d+)?(?:[,，、]\d+(?:[-~至]\d+)?)?周/.test(line) &&
                      !/^\d+(?:[-~至]\d+)?节/.test(line)
                    ) || '';
                    if (!name) continue;
                    const teacher = field(text, ['教师', '老师']);
                    const position = field(text, ['上课地点', '教室', '地点']);
                    const key = [name, day, weeks.join(','), sections.join('-'), teacher, position].join('|');
                    if (seen.has(key)) continue;
                    seen.add(key);
                    courses.push({ name, teacher, position, day, weeks, startSection: sections[0], endSection: sections[1] });
                  }
                }
              });
            }
          }
          if (!matchedTable) return JSON.stringify({ error: '当前页面没有检测到包含星期标题的课表，请先打开本学期课表' });
          if (!courses.length) return JSON.stringify({ error: '找到了课表，但没有识别到周次和节次；请改用图片导入或提交该学校适配' });
          return JSON.stringify({ adapter: 'generic-table-v2', courses, timeSlots: [...timeSlotMap.values()] });
        })()
    """.trimIndent()

    /**
     * Produces a structure-only fingerprint after every course parser fails. It intentionally
     * excludes element values, table cell contents, HTML, cookies and query strings.
     */
    val structureDiagnosticScript: String = """
        (() => {
          const documents = [];
          const collect = (doc, depth) => {
            if (!doc || depth > 5 || documents.includes(doc)) return;
            documents.push(doc);
            for (const frame of doc.querySelectorAll('iframe')) {
              try { collect(frame.contentDocument, depth + 1); } catch (_) {}
            }
          };
          collect(document, 0);
          const redact = value => (value || '')
            .replace(/\d{5,}/g, '<number>')
            .replace(/[A-Za-z0-9_-]{24,}/g, '<token>')
            .slice(0, 80);
          const weekdayOf = value => {
            const text = (value || '').replace(/\s/g, '');
            return [
              ['星期一','周一','Monday','Mon'], ['星期二','周二','Tuesday','Tue'],
              ['星期三','周三','Wednesday','Wed'], ['星期四','周四','Thursday','Thu'],
              ['星期五','周五','Friday','Fri'], ['星期六','周六','Saturday','Sat'],
              ['星期日','星期天','周日','周天','Sunday','Sun']
            ].some(names => names.some(name => text === name || text.includes(name)));
          };
          const selectorCounts = doc => ({
            timetable: doc.querySelectorAll('#timetable').length,
            kbcontent: doc.querySelectorAll('.kbcontent,.kbcontent1').length,
            zhengfangGrid: doc.querySelectorAll('#kbgrid_table_0,[id^="kbgrid_table"]').length,
            timetableCon: doc.querySelectorAll('.timetable_con').length,
            urpClass: doc.querySelectorAll('.class_div').length,
            courseLike: doc.querySelectorAll('[class*="course"],[class*="Course"],[class*="kcb"]').length,
          });
          const documentInfo = documents.map((doc, documentIndex) => {
            let path = '';
            try {
              path = (doc.location.pathname || '').split('/').map(redact).join('/');
            } catch (_) {}
            const tables = Array.from(doc.querySelectorAll('table')).slice(0, 30).map((table, tableIndex) => {
              const rows = Array.from(table.rows || []);
              const weekdayHeaders = rows.reduce((maximum, row) =>
                Math.max(maximum, Array.from(row.cells || []).filter(cell => weekdayOf(cell.innerText)).length), 0);
              return {
                index: tableIndex,
                id: redact(table.id),
                classes: redact(table.className),
                rows: rows.length,
                maxColumns: rows.reduce((maximum, row) => Math.max(maximum, (row.cells || []).length), 0),
                weekdayHeaders,
                rowSpans: table.querySelectorAll('[rowspan]').length,
                colSpans: table.querySelectorAll('[colspan]').length,
              };
            });
            return {
              index: documentIndex,
              path,
              iframeCount: doc.querySelectorAll('iframe').length,
              tables,
              selectors: selectorCounts(doc),
            };
          });
          return JSON.stringify({
            adapter: 'structure-diagnostic-v1',
            documents: documentInfo,
            documentCount: documentInfo.length,
          });
        })()
    """.trimIndent()

    fun parseEvaluationResult(rawResult: String?, school: SchoolSpec): TimetableImportResult {
        if (rawResult.isNullOrBlank() || rawResult == "null") {
            return TimetableImportResult(emptyList(), listOf("教务网页没有返回课表数据"))
        }
        return runCatching {
            val decoded = decodeEvaluationResult(rawResult)
            val root = JSONObject(decoded)
            root.optString("error").takeIf(String::isNotBlank)?.let {
                return TimetableImportResult(emptyList(), listOf(it))
            }
            val pageSectionTimes = mutableMapOf<Int, SectionTime>()
            root.optJSONArray("timeSlots")?.let { slots ->
                for (index in 0 until slots.length()) {
                    val slot = slots.optJSONObject(index) ?: continue
                    val number = slot.optInt("number")
                    val start = parseClock(slot.optString("startTime"))
                    val end = parseClock(slot.optString("endTime"))
                    if (number > 0 && start != null && end != null && end > start) pageSectionTimes[number] = SectionTime(start, end)
                }
            }
            val courses = root.optJSONArray("courses") ?: JSONArray()
            val drafts = buildList {
                for (index in 0 until courses.length()) {
                    parseCourse(courses.getJSONObject(index), school, pageSectionTimes)?.let(::add)
                }
            }
            if (drafts.isEmpty()) TimetableImportResult(emptyList(), listOf("没有识别到课程，请确认已经打开学期理论课表"))
            else TimetableImportResult(drafts, emptyList())
        }.getOrElse { error ->
            TimetableImportResult(emptyList(), listOf("课表解析失败：${error.localizedMessage ?: "数据格式不支持"}"))
        }
    }

    fun diagnosticSummary(rawResult: String?): String {
        if (rawResult.isNullOrBlank() || rawResult == "null") return "empty-result"
        return runCatching {
            val root = JSONObject(decodeEvaluationResult(rawResult))
            val adapter = root.optString("adapter").ifBlank { "unknown" }
            val arrangements = root.optJSONArray("courses")?.length() ?: 0
            val candidates = root.optJSONArray("candidates")
            val candidateText = buildList {
                if (candidates != null) {
                    for (index in 0 until minOf(candidates.length(), 6)) {
                        val item = candidates.optJSONObject(index) ?: continue
                        add(
                            "${item.optString("path").ifBlank { "(no-path)" }}" +
                                ":visible=${item.optBoolean("visible")}" +
                                ",allWeeks=${item.optBoolean("allWeeks")}" +
                                ",arrangements=${item.optInt("arrangements")}" +
                                ",names=${item.optInt("uniqueNames")}" +
                                ",maxWeek=${item.optInt("maxWeek")}" +
                                ",score=${item.optInt("score")}",
                        )
                    }
                }
            }.joinToString(";")
            buildString {
                append("adapter=$adapter arrangements=$arrangements")
                root.optString("error").takeIf(String::isNotBlank)?.let { append(" error=$it") }
                if (candidateText.isNotBlank()) append(" candidates=[$candidateText]")
            }
        }.getOrElse { error ->
            "invalid-result=${error.javaClass.simpleName}"
        }
    }

    fun structureDiagnosticSummary(rawResult: String?): String {
        if (rawResult.isNullOrBlank() || rawResult == "null") return "empty-structure-result"
        return runCatching {
            val root = JSONObject(decodeEvaluationResult(rawResult))
            val documents = root.optJSONArray("documents") ?: JSONArray()
            val summaries = buildList {
                for (documentIndex in 0 until minOf(documents.length(), 8)) {
                    val document = documents.optJSONObject(documentIndex) ?: continue
                    val selectors = document.optJSONObject("selectors") ?: JSONObject()
                    val tables = document.optJSONArray("tables") ?: JSONArray()
                    val tableSummaries = buildList {
                        for (tableIndex in 0 until minOf(tables.length(), 12)) {
                            val table = tables.optJSONObject(tableIndex) ?: continue
                            if (table.optInt("rows") == 0) continue
                            add(
                                "${table.optString("id").ifBlank { "#" + table.optInt("index") }}" +
                                    "(${table.optInt("rows")}x${table.optInt("maxColumns")}" +
                                    ",weekdays=${table.optInt("weekdayHeaders")}" +
                                    ",span=${table.optInt("rowSpans")}/${table.optInt("colSpans")})",
                            )
                        }
                    }.joinToString(",")
                    add(
                        "doc${document.optInt("index")}" +
                            " path=${document.optString("path").ifBlank { "/" }}" +
                            " frames=${document.optInt("iframeCount")}" +
                            " selectors=t${selectors.optInt("timetable")}" +
                            "/k${selectors.optInt("kbcontent")}" +
                            "/z${selectors.optInt("zhengfangGrid")}" +
                            "/c${selectors.optInt("timetableCon")}" +
                            "/u${selectors.optInt("urpClass")}" +
                            " tables=[$tableSummaries]",
                    )
                }
            }
            "adapter=${root.optString("adapter").ifBlank { "unknown" }} documents=${root.optInt("documentCount")} ${summaries.joinToString(";")}"
        }.getOrElse { error ->
            "invalid-structure-result=${error.javaClass.simpleName}"
        }
    }

    private fun decodeEvaluationResult(rawResult: String): String =
        if (rawResult.startsWith('"')) JSONObject("{\"value\":$rawResult}").getString("value") else rawResult

    private fun parseCourse(json: JSONObject, school: SchoolSpec, pageSectionTimes: Map<Int, SectionTime>): CourseDraft? {
        val name = json.optString("name").trim()
        val dayValue = json.optInt("day")
        val startSection = json.optInt("startSection")
        val endSection = json.optInt("endSection")
        if (name.isBlank() || dayValue !in 1..7 || startSection <= 0 || endSection < startSection) return null
        val weeksJson = json.optJSONArray("weeks") ?: JSONArray()
        val weeks = (0 until weeksJson.length()).map { weeksJson.optInt(it) }.filter { it in 1..63 }.distinct().sorted()
        if (weeks.isEmpty()) return null
        val courseStart = parseClock(json.optString("startTime"))
        val courseEnd = parseClock(json.optString("endTime"))
        val start = courseStart
            ?: school.sectionTimes[startSection]?.startMinutes
            ?: pageSectionTimes[startSection]?.startMinutes
            ?: defaultStart(startSection)
        val end = courseEnd
            ?: school.sectionTimes[endSection]?.endMinutes
            ?: pageSectionTimes[endSection]?.endMinutes
            ?: defaultEnd(endSection)
        return CourseDraft(
            name = name,
            day = DayOfWeek.of(dayValue),
            startMinutes = start,
            endMinutes = end,
            location = json.optString("position").trim(),
            teacher = json.optString("teacher").trim(),
            startWeek = weeks.first(),
            endWeek = weeks.last(),
            weekPattern = inferPattern(weeks),
            weekMask = weeks.fold(0L) { mask, week -> mask or (1L shl (week - 1)) },
        )
    }

    private fun inferPattern(weeks: List<Int>): WeekPattern = when {
        weeks.all { it % 2 == 1 } -> WeekPattern.ODD
        weeks.all { it % 2 == 0 } -> WeekPattern.EVEN
        else -> WeekPattern.ALL
    }

    private fun defaultStart(section: Int): Int = 8 * 60 + (section - 1) * 50
    private fun defaultEnd(section: Int): Int = defaultStart(section) + 45
    private fun parseClock(value: String): Int? {
        val parts = value.trim().split(':')
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        return if (hour in 0..23 && minute in 0..59) hour * 60 + minute else null
    }
}
