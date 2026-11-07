#!/usr/bin/env python3
"""Local catalogue/enquiry smoke check. Uses synthetic data and requires an explicit adb serial.
Build with apiBaseUrl=http://127.0.0.1:3000; run the demo backend and Mailpit first.
--smtp-failure uses an owned temporary backend with a closed SMTP port, never stops Mailpit.
"""
import argparse
import json
import os
from pathlib import Path
import re
import shutil
import socket
import subprocess
import tempfile
import time
import urllib.request
import uuid
import xml.etree.ElementTree as ET

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--serial', required=True)
parser.add_argument('--output', type=Path)
parser.add_argument('--adb', default=shutil.which('adb') or str(Path.home() / 'Library/Android/sdk/platform-tools/adb'))
parser.add_argument('--smtp-failure', action='store_true')
args = parser.parse_args()
args.output = args.output or Path(tempfile.mkdtemp(prefix='catalogue-smoke-'))
args.output.mkdir(parents=True, exist_ok=True)
repo = Path(__file__).resolve().parents[1]
adb_command = [args.adb, '-s', args.serial]
activity = 'com.cataloguestudio/.MainActivity'
package = 'com.cataloguestudio'
owned_backend = None
owned_log = None
marker = f'Catalogue smoke {uuid.uuid4().hex[:12]}'

def adb(*parts):
    return subprocess.check_output(adb_command + list(parts), timeout=25)

def get(url):
    with urllib.request.urlopen(url, timeout=6) as response:
        return json.load(response)

def dump():
    adb('shell', 'uiautomator', 'dump', '/sdcard/catalogue-smoke.xml')
    return ET.fromstring(adb('exec-out', 'cat', '/sdcard/catalogue-smoke.xml'))

def texts(root):
    return [n.get('text', '') for n in root.iter('node') if n.get('package') == package]

def bounds(node):
    return list(map(int, re.findall(r'\d+', node.get('bounds'))))

def tap(node):
    x1, y1, x2, y2 = bounds(node)
    adb('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))

def swipe(direction='up'):
    size = adb('shell', 'wm', 'size').decode().strip().splitlines()[-1]
    width, height = map(int, re.findall(r'(\d+)x(\d+)', size)[0])
    start, end = ((.75, .32) if direction == 'up' else (.30, .75))
    adb('shell', 'input', 'swipe', str(width//2), str(int(height*start)), str(width//2), str(int(height*end)), '250')

def find(label, direction='up'):
    for _ in range(7):
        root = dump()
        node = next((n for n in root.iter('node') if n.get('package') == package and n.get('text') == label), None)
        if node is not None:
            return root, node
        swipe(direction)
    raise AssertionError(f'Could not find {label!r}; visible app text: {texts(root)}')

def click(label, direction='up'):
    root, node = find(label, direction)
    tap(node)
    return root

def shot(name):
    (args.output / f'{name}.png').write_bytes(adb('exec-out', 'screencap', '-p'))

def fill(label, value):
    root, label_node = find(label)
    candidates = []
    for parent in root.iter('node'):
        descendants = list(parent.iter('node'))
        if label_node in descendants:
            fields = [n for n in descendants if n.get('class') == 'android.widget.EditText']
            if fields:
                candidates.append((len(descendants), fields[0]))
    assert candidates, f'No editable field for {label}'
    tap(min(candidates, key=lambda x: x[0])[1])
    time.sleep(.3)
    adb('shell', 'input', 'text', value.replace(' ', '%s'))
    # The submit action must remain visible above the open keyboard.
    assert 'Send enquiry' in texts(dump()), 'Submit button is hidden by the keyboard'
    adb('shell', 'input', 'keyevent', '4')

def restore_process():
    adb('shell', 'input', 'keyevent', '3')
    time.sleep(1)
    adb('shell', 'am', 'kill', package)
    remaining = subprocess.run(adb_command + ['shell', 'pidof', package], capture_output=True, timeout=5)
    assert not remaining.stdout.strip(), 'Background app process was not killed; restoration not established'
    adb('shell', 'am', 'start', '-n', activity)
    # A new process must restore the enquiry route and inputs, without resending.
    find('Let’s talk details.', direction='down')

try:
    assert get('http://127.0.0.1:3000/health')['status'] == 'ok'
    catalogue = get('http://127.0.0.1:3000/api/catalogue')
    assert catalogue['mode'] == 'demo', 'Use PRODUCT_SOURCE=demo for this smoke check'
    selected = catalogue['featured'][0]
    before = {m['ID'] for m in get('http://127.0.0.1:8025/api/v1/messages')['messages']}
    adb('reverse', 'tcp:3000', 'tcp:3000')
    adb('shell', 'am', 'start', '-S', '-n', activity)
    click('Explore products')
    click(selected['title'])
    find('Product details', direction='down')
    shot('product-details')
    click('Enquire about this product')
    find(selected['title'])
    click('Send enquiry')
    find('Please enter your name.')
    find('Please enter your email address.')
    assert before == {m['ID'] for m in get('http://127.0.0.1:8025/api/v1/messages')['messages']}, 'Invalid form sent email'
    print('PASS: selected product opens detail/form; invalid fields do not send email', flush=True)
    # Return to the top of the form before filling fields in order.
    find('Full name', direction='down')
    fill('Full name', 'Catalogue Review')
    fill('Email address', 'catalogue.review@example.test')
    fill('Message (optional)', marker)
    shot('filled-form')
    if args.smtp_failure:
        with socket.socket() as allocation:
            allocation.bind(('127.0.0.1', 0))
            port = allocation.getsockname()[1]
        env = dict(os.environ, HOST='127.0.0.1', PORT=str(port), PRODUCT_SOURCE='demo', SMTP_HOST='127.0.0.1', SMTP_PORT='1', SMTP_SECURE='false')
        owned_log = (args.output / 'temporary-backend.log').open('w')
        owned_backend = subprocess.Popen([shutil.which('node') or 'node', 'dist/server.js'], cwd=repo/'backend', env=env, stdout=owned_log, stderr=subprocess.STDOUT)
        for _ in range(20):
            try:
                get(f'http://127.0.0.1:{port}/health')
                break
            except OSError:
                time.sleep(.2)
        else:
            raise AssertionError('Temporary failure backend did not become ready')
        adb('reverse', 'tcp:3000', f'tcp:{port}')
        restore_process()
        click('Send enquiry')
        find('The service is having trouble. Please try again later.')
        assert marker in texts(dump()), 'Failed form lost its message'
        shot('smtp-failure-retained-form')
        assert before == {m['ID'] for m in get('http://127.0.0.1:8025/api/v1/messages')['messages']}, 'Failed SMTP unexpectedly added mail'
        print('PASS: real SMTP failure retains restored form and does not show confirmation', flush=True)
        adb('reverse', 'tcp:3000', 'tcp:3000')
        owned_backend.terminate()
        owned_backend.wait(timeout=8)
        owned_backend = None
    restore_process()
    find('Catalogue Review')
    find('catalogue.review@example.test')
    find(marker)
    print('PASS: new process restores the route and all draft inputs without automatic submission', flush=True)
    click('Send enquiry')
    find('Enquiry received')
    assert not any(n.get('class') == 'android.widget.EditText' for n in dump().iter('node'))
    shot('enquiry-received')
    messages = get('http://127.0.0.1:8025/api/v1/messages')['messages']
    matches = []
    for message in messages:
        if message['ID'] not in before:
            detail = get(f"http://127.0.0.1:8025/api/v1/message/{message['ID']}")
            if marker in detail['Text']:
                matches.append(detail)
    assert len(matches) == 1, 'Expected exactly one captured email for this successful test submission'
    assert matches[0]['To'][0]['Address'] == 'sales@catalogue-studio.test'
    assert matches[0]['ReplyTo'][0]['Address'] == 'catalogue.review@example.test'
    assert f"Product ID: {selected['id']}" in matches[0]['Text']
    assert selected['title'] in matches[0]['Text']
    click('Back to product')
    find('Product details', direction='down')
    find(selected['title'])
    click('Back', direction='down')
    find('Catalogue Studio', direction='down')
    shot('catalogue-return')
    print('PASS: backend acceptance shows confirmation; one matching Mailpit email; Back returns through detail to catalogue', flush=True)
    print(f'Screenshots: {args.output}', flush=True)
finally:
    # Restore the normal development connection even after an assertion fails.
    subprocess.run(adb_command + ['reverse', 'tcp:3000', 'tcp:3000'], capture_output=True, timeout=5)
    if owned_backend is not None:
        owned_backend.terminate()
        owned_backend.wait(timeout=8)
    if owned_log is not None:
        owned_log.close()
