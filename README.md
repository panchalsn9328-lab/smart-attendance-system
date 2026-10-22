# Smart Attendance System

An ESP32 and Bluetooth Low Energy (BLE)-based Smart Attendance System designed to simplify classroom attendance using an Android application and Firebase.

## 📌 Project Overview

The Smart Attendance System aims to make classroom attendance more convenient and efficient by using BLE technology. The ESP32 device acts as a classroom beacon, while the Android application detects its presence to support attendance recording.

The system is designed to reduce manual attendance work and provide a digital approach to attendance management.

## 🎯 Objectives

- Automate classroom attendance using BLE technology.
- Use ESP32 as the classroom attendance device.
- Develop an Android application for students.
- Integrate Firebase for attendance data storage and synchronization.
- Support offline attendance storage when Wi-Fi is unavailable.
- Synchronize locally stored attendance records when the network becomes available.
- Reduce manual work and improve attendance management.

## 🛠️ Technologies Used

- **Hardware:** ESP32 Development Board
- **Wireless Technology:** Bluetooth Low Energy (BLE)
- **Mobile Application:** Android
- **Database and Backend:** Firebase
- **Programming Languages:** Based on the implemented Android and ESP32 firmware
- **Development Tools:** Android Studio / Arduino IDE, as applicable

## ⚙️ System Features

- BLE-based classroom device detection.
- Student login through the Android application.
- ESP32-controlled attendance session.
- Digital attendance record management.
- Offline record storage and later synchronization.
- Firebase integration for cloud-based data storage.

*Note: Features may depend on the current implementation of the hardware, Android application, and Firebase backend.*

## 🏗️ System Architecture

1. The ESP32 device is installed in the classroom.
2. The ESP32 broadcasts or exposes its BLE presence.
3. The student's Android application detects the classroom device.
4. Attendance is recorded according to the configured attendance-session logic.
5. Attendance records are stored locally when offline, if supported by the firmware.
6. Records are synchronized with Firebase when connectivity returns.

## 📂 Project Structure

```text
smart-attendance-system/
├── android-app/
├── esp32-firmware/
├── documentation/
├── screenshots/
└── README.md
```

The folder structure may be adjusted to match the actual project files.

## 🚀 Getting Started

### Prerequisites

- ESP32 development board
- Android smartphone
- Bluetooth enabled on the smartphone
- Required Android development environment
- Arduino IDE or compatible ESP32 development environment
- Firebase project with appropriate configuration

### Installation

1. Clone or download this repository.
2. Open the ESP32 firmware in the appropriate development environment.
3. Configure the required Wi-Fi and Firebase settings locally.
4. Upload the firmware to the ESP32 board.
5. Open the Android application source code in Android Studio or the compatible IDE.
6. Configure Firebase according to the project's setup requirements.
7. Build and install the Android application on a compatible device.
8. Test BLE detection and attendance recording.

## 🔐 Security

- Do not commit passwords, private keys, service-account credentials, or other secrets.
- Configure Firebase Authentication and database security rules appropriately.
- Avoid publishing real student information or attendance records.
- Store sensitive configuration outside the public repository.

## 🔮 Future Enhancements

- Improved attendance verification.
- Attendance reports and analytics.
- Automated attendance summaries for faculty.
- Better offline synchronization and error handling.
- Enhanced application interface and user experience.

## 👨‍💻 Project Information

**Project Name:** Smart Attendance System

**Domain:** Internet of Things (IoT) and Mobile Application Development

**Hardware Platform:** ESP32

**Communication Technology:** Bluetooth Low Energy (BLE)

## 📄 License

This project is available for educational and learning purposes. Add an appropriate open-source license if you intend to permit reuse or redistribution.

## ⭐ Acknowledgement

This project explores the use of IoT, BLE communication, Android development, and cloud technologies to improve the classroom attendance process.
