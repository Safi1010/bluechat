BlueChat 📡

Bluetooth-based Chat Application for Android
No Internet Required — Communicate directly with nearby devices via Bluetooth


📌 Project Overview

BlueChat is an Android application that enables two nearby devices to communicate through Bluetooth without using the internet. The app provides a simple Host–Join mechanism using QR codes to establish a secure and fast connection between devices, followed by real-time text chat.

This project demonstrates Bluetooth communication, QR-based device pairing, and real-time messaging in Android.

Use Cases

Communication in areas with no internet

Quick device-to-device chatting

Learning Bluetooth & QR integration in Android


🚀 Features

✔ Dashboard with two options: Host or Join
✔ Host Mode

Generates a QR code containing connection details

Waits for another device to connect

✔ Join Mode

Opens a QR code scanner

Scans the Host’s QR code to connect automatically

✔ Automatic Chat Opening

After successful QR scan and Bluetooth connection

Chat screen opens on both connected devices

✔ Real-time Bluetooth text messaging
✔ Simple and user-friendly interface
✔ Works completely offline (no Wi-Fi / mobile data)

Note: Bluetooth must be enabled on both devices.


🧱 Technologies Used
Technology	Purpose
Android Studio	Development environment
Android Bluetooth API	Device discovery & communication
QR Code Generator & Scanner	Quick Host–Join connection
Java / Kotlin	Application logic
XML	UI design
Gradle	Build & dependency management


📷 Screenshots

Dashboard Screen

![WhatsApp Image 2025-12-19 at 6 51 28 PM](https://github.com/user-attachments/assets/1275c21b-9a74-4f41-9807-8399477436f1)

Host screen

![WhatsApp Image 2025-12-19 at 6 51 28 PM (1)](https://github.com/user-attachments/assets/1083ae39-9062-4af1-a5dd-b0870f978c94)


🛠️ How the App Works (Flow)

User opens the app → Dashboard

User selects:

Host → QR code is generated

Join → QR scanner opens

Join device scans Host’s QR code

Bluetooth connection is established automatically

Chat screen opens on both devices

Users can exchange messages in real time

⚙️ How to Run the Project

Clone the repository:

git clone https://github.com/Safi1010/bluechat.git


Open the project in Android Studio

Let Gradle sync completely

Enable Bluetooth on both Android devices

Run the app on two physical devices

Use Host on one device and Join on the other


🔐 Permissions Used

BLUETOOTH

BLUETOOTH_ADMIN

BLUETOOTH_CONNECT

BLUETOOTH_SCAN

ACCESS_FINE_LOCATION (required for Bluetooth discovery & QR flow)


📁 Project Structure
bluechat/
├── app/
│   ├── src/main/java/
│   │   ├── activities/
│   │   ├── bluetooth/
│   │   └── qr/
│   ├── src/main/res/
│   │   └── layout/
│   ├── AndroidManifest.xml
├── screenshots/
├── build.gradle
└── README.md

👨‍👩‍👧‍👦 Project Members: 
Saif ul Islam, 
Muhammad Huzefa 
---
