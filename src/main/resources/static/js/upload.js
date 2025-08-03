function generateQR() {
    const codeElement = document.querySelector('.share-code');
    if (!codeElement) return;

    const code = codeElement.textContent.trim();
    const ip = window.deviceIP || 'your-ip';
    const url = `http://${ip}:8080/info/${code}`;

    const qrDiv = document.getElementById('qrcode');
    qrDiv.innerHTML = ''; // clear previous QR

    const qrCanvas = document.createElement('canvas');
    qrDiv.appendChild(qrCanvas);

    new QRious({
        element: qrCanvas,
        value: url,
        size: 200,
        background: 'white',
        foreground: 'black'
    });
}
