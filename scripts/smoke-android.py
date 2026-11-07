#!/usr/bin/env python3
"""Exercise the installed app against local Compose services; use a disposable emulator."""
import argparse
import json
import re
import subprocess
import time
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--serial', default='emulator-5554')
parser.add_argument('--smtp-failure', action='store_true', help='Briefly stop this project\'s Mailpit service to test recovery')
args = parser.parse_args()
ADB = ['adb', '-s', args.serial]
SHOTS = ROOT / 'docs/screenshots'
SHOTS.mkdir(parents=True, exist_ok=True)


def adb(*parts, binary=False):
    return subprocess.check_output(ADB + list(parts), timeout=30, text=not binary)


def tree():
    adb('shell', 'uiautomator', 'dump', '/sdcard/catalogue-studio-smoke.xml')
    return ET.fromstring(adb('exec-out', 'cat', '/sdcard/catalogue-studio-smoke.xml'))


def find(attribute, value, scroll=False):
    for attempt in range(7 if scroll else 1):
        for node in tree().iter('node'):
            if node.get(attribute) == value and node.get('bounds') != '[0,0][0,0]':
                return node
        if scroll:
            swipe()
    raise AssertionError(f'Cannot find {attribute}={value!r}')


def wait_text(text):
    deadline = time.monotonic() + 30
    while time.monotonic() < deadline:
        if any(n.get('text') == text for n in tree().iter('node')):
            return
        time.sleep(.25)
    raise AssertionError(f'Did not see {text!r}')


def tap_node(node):
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.get('bounds')))
    adb('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))


def tap_id(value, scroll=False):
    tap_node(find('resource-id', value, scroll))


def tap_text(value):
    tap_node(find('text', value))


def swipe():
    width, height = map(int, re.findall(r'(\d+)x(\d+)', adb('shell', 'wm', 'size'))[-1])
    adb('shell', 'input', 'swipe', str(width//2), str(int(height*.78)), str(width//2), str(int(height*.35)), '350')


def screenshot(name):
    # Wait for local image decodes and navigation transitions before capture.
    time.sleep(1)
    (SHOTS / f'{name}.png').write_bytes(adb('exec-out', 'screencap', '-p', binary=True))
    print(f'Captured {name}', flush=True)


def fill_form(email):
    actual_email = email
    for tag, value in [('name_input', 'Android Smoke'), ('email_input', email), ('message_input', 'Please tell me more about this piece.')]:
        tap_id(tag, scroll=True)
        adb('shell', 'input', 'text', value.replace(' ', '%s'))
        adb('shell', 'input', 'keyevent', 'KEYCODE_BACK')
        if tag == 'email_input':
            # adb-injected text can be capitalised by the emulator IME. Verify the value actually submitted.
            actual_email = find('resource-id', tag).get('text') or email
    return actual_email


def inbox_json(path):
    with urllib.request.urlopen('http://127.0.0.1:8025'+path, timeout=10) as response:
        return json.load(response)


def main():
    adb('shell', 'am', 'force-stop', 'com.cataloguestudio')
    adb('shell', 'am', 'start', '-n', 'com.cataloguestudio/.MainActivity')
    wait_text('The studio edit')
    screenshot('home')
    tap_text('Men')
    wait_text('Top rated')
    screenshot('category')
    tap_id('search_input')
    adb('shell', 'input', 'text', 'cotton')
    tap_node(find('content-desc', 'Search products'))
    wait_text('Results for “cotton”')
    assert any(n.get('text') == 'Essential cotton henley' for n in tree().iter('node'))
    assert not any(n.get('text') == 'Everyday canvas backpack' for n in tree().iter('node'))
    screenshot('search')
    tap_text('All pieces')
    wait_text('The studio edit')
    tap_id('product_1', scroll=True)
    wait_text('The details')
    screenshot('detail')
    tap_id('enquire_button')
    wait_text('Your name')
    screenshot('enquiry')
    email = f'android.smoke.{int(time.time())}@example.test'
    email = fill_form(email)
    tap_id('send_enquiry', scroll=True)
    wait_text('Thank you.')
    screenshot('success')
    found = False
    for message in inbox_json('/api/v1/messages')['messages']:
        if 'Everyday canvas backpack' in message['Subject']:
            full = inbox_json('/api/v1/message/'+message['ID'])
            if email in full['Text']:
                found = True
                break
    assert found, 'Android enquiry did not appear in local sales inbox'
    print('PASS: category, search, detail, enquiry, thank-you, and actual local SMTP delivery', flush=True)

    if args.smtp_failure:
        tap_text('Back to the piece')
        tap_id('enquire_button')
        wait_text('Your name')
        fill_form('android.failure@example.test')
        subprocess.run(['docker', 'compose', 'stop', 'mailpit'], cwd=ROOT, check=True)
        try:
            tap_id('send_enquiry', scroll=True)
            wait_text('Your enquiry could not be confirmed. Your details are still here; please try again.')
            assert not any(n.get('text') == 'Thank you.' for n in tree().iter('node'))
            screenshot('enquiry-failure')
        finally:
            subprocess.run(['docker', 'compose', 'start', 'mailpit'], cwd=ROOT, check=True)
        # The same form must be retryable without re-entering contact details.
        tap_id('send_enquiry', scroll=True)
        wait_text('Thank you.')
        print('PASS: SMTP failure preserves form; retry succeeds after inbox restarts', flush=True)


try:
    main()
except Exception:
    (ROOT / 'work').mkdir(exist_ok=True)
    (ROOT / 'work/smoke-failed.png').write_bytes(adb('exec-out', 'screencap', '-p', binary=True))
    raise
