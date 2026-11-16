// Проверка настоящих обработчиков HTML без браузера: Canvas скрыт (ширина 0).
const assert = require('node:assert/strict');
const {readFileSync} = require('node:fs');
const {test} = require('node:test');
const vm = require('node:vm');

function loadUi() {
    const html = readFileSync(`${__dirname}/../web/potential-well.html`, 'utf8');
    const elements = new Map();
    for (const match of html.matchAll(/<[^>]+\bid="([^"]+)"[^>]*>/g)) {
        const value = match[0].match(/\bvalue="([^"]+)"/);
        elements.set(match[1], {value: value?.[1] ?? '', clientWidth: 0, clientHeight: 0,
            textContent: '', listeners: {}, addEventListener(type, listener) { this.listeners[type] = listener; }});
    }
    vm.runInNewContext(html.match(/<script>([\s\S]*?)<\/script>/)[1], {
        document: {getElementById: id => elements.get(id), querySelectorAll: () => []},
        window: {addEventListener() {}}, requestAnimationFrame: callback => callback(),
    });
    return elements;
}

test('снижение N синхронизирует input n, max и подпись энергии', () => {
    const ui = loadUi();
    ui.get('level').value = '5';
    ui.get('level').listeners.input();
    ui.get('count').value = '2';
    ui.get('count').listeners.input();
    assert.equal(ui.get('level').value, '2');
    assert.equal(ui.get('level').max, '2');
    assert.match(ui.get('selectedEnergy').textContent, /^E2 = /);
    ui.get('count').value = '1';
    ui.get('count').listeners.input();
    assert.equal(ui.get('level').value, '1');
    ui.get('count').value = '10';
    ui.get('count').listeners.input();
    assert.equal(ui.get('level').value, '1');
});

test('дробные номера и значения вне диапазона согласованы с графиком', () => {
    const ui = loadUi();
    ui.get('count').value = '2.6';
    ui.get('level').value = '99';
    ui.get('count').listeners.input();
    assert.equal(ui.get('count').value, '3');
    assert.equal(ui.get('level').value, '3');
    assert.match(ui.get('selectedEnergy').textContent, /^E3 = /);
});
