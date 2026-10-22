#include <Arduino.h>
#include <Wire.h>
#include <LiquidCrystal_I2C.h>

// LCD 16x2 I2C
LiquidCrystal_I2C lcd(0x27, 16, 2);

// CH375B data pins D0-D7
const int D[8] = {13, 14, 15, 2, 18, 19, 23, 25};

// CH375B control pins
const int WR_PIN = 26;
const int RD_PIN = 27;
const int CS_PIN = 32;
const int A0_PIN = 33;
const int INT_PIN = 34;

void busMode(uint8_t mode) {
  for (int i = 0; i < 8; i++) {
    pinMode(D[i], mode);
  }
}

void writeCommand(uint8_t command) {
  busMode(OUTPUT);

  for (int i = 0; i < 8; i++) {
    digitalWrite(D[i], (command >> i) & 1);
  }

  digitalWrite(A0_PIN, HIGH);
  digitalWrite(CS_PIN, LOW);
  digitalWrite(WR_PIN, LOW);
  delayMicroseconds(5);
  digitalWrite(WR_PIN, HIGH);
  digitalWrite(CS_PIN, HIGH);
}

void writeData(uint8_t data) {
  busMode(OUTPUT);

  for (int i = 0; i < 8; i++) {
    digitalWrite(D[i], (data >> i) & 1);
  }

  digitalWrite(A0_PIN, LOW);
  digitalWrite(CS_PIN, LOW);
  digitalWrite(WR_PIN, LOW);
  delayMicroseconds(5);
  digitalWrite(WR_PIN, HIGH);
  digitalWrite(CS_PIN, HIGH);
}

uint8_t readData() {
  busMode(INPUT);

  digitalWrite(A0_PIN, LOW);
  digitalWrite(CS_PIN, LOW);
  digitalWrite(RD_PIN, LOW);
  delayMicroseconds(5);

  uint8_t value = 0;

  for (int i = 0; i < 8; i++) {
    if (digitalRead(D[i])) {
      value |= (1 << i);
    }
  }

  digitalWrite(RD_PIN, HIGH);
  digitalWrite(CS_PIN, HIGH);

  return value;
}

bool checkCH375() {
  writeCommand(0x06);  // CHECK_EXIST
  writeData(0x55);
  delay(2);

  uint8_t response = readData();

  Serial.print("CH375B Response: 0x");
  Serial.println(response, HEX);

  return response == 0xAA;
}

void showLCD(const char* line1, const char* line2) {
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print(line1);
  lcd.setCursor(0, 1);
  lcd.print(line2);
}

void setup() {
  Serial.begin(115200);

  // LCD setup
  Wire.begin(21, 22);
  lcd.init();
  lcd.backlight();

  showLCD("Complaint Box", "Starting...");
  delay(1500);

  // CH375B control pins
  pinMode(WR_PIN, OUTPUT);
  pinMode(RD_PIN, OUTPUT);
  pinMode(CS_PIN, OUTPUT);
  pinMode(A0_PIN, OUTPUT);
  pinMode(INT_PIN, INPUT);

  digitalWrite(WR_PIN, HIGH);
  digitalWrite(RD_PIN, HIGH);
  digitalWrite(CS_PIN, HIGH);

  busMode(INPUT);

  Serial.println("ESP32 + LCD + CH375B");
  delay(500);

  if (checkCH375()) {
    Serial.println("CH375B CHECK_EXIST PASS");
    showLCD("CH375B Connected", "Check: PASS");
  } else {
    Serial.println("CH375B CHECK_EXIST FAIL");
    showLCD("CH375B Error", "Check Wiring");
  }
}

void loop() {
  // Status LCD par display hota rahega.
}