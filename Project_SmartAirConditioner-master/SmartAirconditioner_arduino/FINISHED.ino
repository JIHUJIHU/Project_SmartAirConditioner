#include <DS1302.h>
#include <WiFiEsp.h>
#include <LiquidCrystal_I2C.h>
#include <Timer.h> //timer
#include <Time.h> //rtc
#include <TimeLib.h>  //rtc
#include <SoftwareSerial.h>
#ifndef HAVE_HWSERIAL1
SoftwareSerial Serial1(18, 19); // wifi TX, RX
#endif
#define BUFF_SIZE 5
// RTC 핀들-------
#define SCK_PIN 10 
#define IO_PIN 9
#define RST_PIN 8
//------------------
//LED 핀들 ----
#define led_blue 52
#define led_green 50
#define led_yellow 48
#define led_red 46
#define rxPin 13
#define txPin 12
#define motorPin 6
SoftwareSerial bt(txPin, rxPin);  //블루투스


char ssid[] = "CHwifiegg";            // your network SSID (name)
char pass[] = "changhyub2";        // your network password
int status = WL_IDLE_STATUS;     // the Wifi radio's status
int pm10 = 0; //미세먼지 
String wt_temp = ""; //미세먼지 농도 읽은 값 (string 형)
int pmValue=0; //미세먼지 농도 값 (int로 변환한 값)
int p_temp; //미세먼지 임시 농도 값
char Buffer[5];
char server[] = "openapi.airkorea.or.kr";

// Initialize the Ethernet client object
WiFiEspClient client;
Timer ts; //타이머


LiquidCrystal_I2C lcd(0x27, 16, 2);  // I2C LCD 객체 선언
char buffer[BUFF_SIZE];  // 블루투스로 상태 보내기

DS1302 rtc(RST_PIN, IO_PIN, SCK_PIN); //RTC 
String fanlevel=""; //팬 레벨
String val; // 바이트 값 스트링으로 변환
byte data; // 블루투스로 받을값
byte buff[BUFF_SIZE]={'\0'}; //블루투스로 받을 버퍼
int bufferPosition; //버퍼 데이터 저장 위치
Time t; // rtc 타이머로 팬 끄기
boolean temp_t = false; //타이머 들어왔는지 확인
int timer; //타이머 시간
boolean auto_ =true; //1 = 자동 2 = 수동
int speed; //팬세기

//----미세먼지 값 읽어오는 함수-------------------------------
void read_pm();
void post(String a);
///////////////////////////////////////////////////////////////////
void setup()
{
  pinMode(motorPin, OUTPUT);
  pinMode(led_blue, OUTPUT);
  pinMode(led_green, OUTPUT);
  pinMode(led_yellow, OUTPUT);
  pinMode(led_red, OUTPUT);
  // initialize serial for debugging
  Serial.begin(9600); //시리얼
  bt.begin(9600); //블루투스
  // initialize serial for ESP module
  Serial1.begin(9600); //와이파이
  // initialize ESP module
  WiFi.init(&Serial1);

  // check for the presence of the shield
  if (WiFi.status() == WL_NO_SHIELD) {
    Serial.println(F("WiFi shield not present")); //쉴드 인식
    // don't continue
    while (true);
  }
  lcd.begin(); // lcd를 사용을 시작합니다.
  lcd.backlight(); // backlight를 On 시킵니다.
  lcd.clear();
  lcd.setCursor(0,0); //커서
  lcd.print("wait....");
  read_pm();
  ts.every(60000,read_pm); //1초에 한번 미세먼지 
}

void loop()
{
   if(bt.available()){
    data = bt.read(); //전송값 읽기
    buff[bufferPosition++] = data; // 수신 받은 데이터를 버퍼에 저장
    if(data == '#'){ //문자열 종료 표시
      bufferPosition = 0;
      for(char c : buff)
        val += c;
      //자동 수동 받기---------------------------------
      if(val == "M1#"){
 //       Serial.println("M1");
        auto_ = true;
      }else if(val == "M0#"){
  //      Serial.println("M2");
        auto_ = false;
      }
      //수동 모드----------------------------------
      if(auto_ == false){
      // 신호에 따라 스피드 
        if (val == "P0#" || val == "P1#" || val == "P2#" || val == "P3#")
         {
          if(val == "P0#"){
            speed = 0;
            fanlevel = "C0#";
          }else if(val == "P1#"){
            speed = 130;
            fanlevel = "C1#";
          }else if(val == "P2#"){
            speed = 200;
            fanlevel = "C2#";
          }else if(val == "P3#"){
            speed = 255;
            fanlevel = "C3#";
          }
//          //LCD출력 용 레벨


          //버퍼 초기화-----------
          for(int i=0;i<5;i++){
            buff[i]='\0';
          }
      //---------------------------------------------------
          for(int cnt = 0;cnt<30000; cnt++){ //초반에 힘딸려서.
            analogWrite(motorPin, 255);
          }
          analogWrite(motorPin, speed); //팬 작동
          post(fanlevel); //앱으로 fan 레벨 전송
        }
      //-----------팬 제어 끝----------------------------------------------
      
      //--------타이머 셑--------------------------------------------------
      else if(val == "T1#" || val == "T2#" || val == "T3#" || val == "T4#" || val == "T5#" || val == "T6#" || val == "T7#" || val == "T8#" || val == "T9#"){
        //버퍼 두번째 (숫자) 읽어서 char로 바꾸고 다시 int로 바꿈 (타이머)
        char tem = buff[1];
        timer = (int)tem - 48;
        Serial.print(timer);
        rtc.setTime(0,0,0); //rtc 시간을 0으로 초기화
        temp_t = true; //타이머 들어왔다고 알림
      }
     }
   }
   val = ""; //val 초기화
    
  }//블루투스통신 중괄호

//자동 모드 ---------------------------------

   if(auto_ == true){
  //wifi로 api 받은다음 그걸로 팬 스피드 정하기..
  //블루투스 연결끊겼을때도 돌아가게 할려면.. 
    if(p_temp <= 30){
      speed = 0;
    }else if(p_temp >30 && p_temp<=50){
      speed = 130;
    }else if(p_temp > 50 && p_temp<=100){
      speed = 200 ;
    }else if(p_temp >100){
      speed = 255; 
    }
    if(speed==0){
      analogWrite(motorPin,speed);
    }
    else{
      for(int cnt = 0;cnt<30000; cnt++){ //초반에 힘딸려서.
        analogWrite(motorPin, 255);
      }
      analogWrite(motorPin,speed);
    }

  }

//-------led 켜기---------------------------------
  if(speed == 0){ 
     digitalWrite(led_blue,HIGH);
  }else if(speed == 130){
     digitalWrite(led_green,HIGH);
  }else if(speed == 200){
     digitalWrite(led_yellow,HIGH);
  }else if(speed == 255){
     digitalWrite(led_red,HIGH);
  }
//-------led 끄기---------------------------------
   if(speed != 0){
     digitalWrite(led_blue,LOW);
   }if(speed != 130){
     digitalWrite(led_green,LOW);
   }if(speed != 200){
     digitalWrite(led_yellow,LOW);
   }if(speed != 255){
      digitalWrite(led_red,LOW);
   }
  // 타이머 ------------------------------------------------------------
  t = rtc.getTime(); //현재 rtc 시간 받아옴
  if(t.hour == timer && temp_t == true){//현재 rtc 시간의 시간과 타이머의 시간이 일치 & 타이머가 세팅
      speed = 0;
      analogWrite(motorPin,0);
      post("C0#");
      timer;
      temp_t = false;
   }

   
//LCD----------------------------------------------------------------------------
  //lcd 출력
  lcd.setCursor(0,0);
  lcd.print("FineDust = ");
  lcd.setCursor(11,0);
  lcd.print(p_temp);
  lcd.print(" pm");
  lcd.setCursor(0,1);//두번째 줄
  lcd.print("FAN =  ");
  lcd.setCursor(6,1);
  if(speed == 0){
    lcd.print("OFF                     ");
  }else{
    lcd.print("POWER  ");
    if(speed == 130){
      lcd.print("1");
    }else if(speed == 200){
      lcd.print("2");
    }else if(speed == 255){
      lcd.print("3");
    }
    lcd.print("      ");
  }
  ts.update();
}



//블루투스. 앱으로 상태보내기-------------------------------------
void post(String a){
  String s = a;
  s.toCharArray(buffer, BUFF_SIZE);
  buffer[BUFF_SIZE] = '#';
  for(int i = 0; i<5; i++){
    bt.write(buffer[i]);
    Serial.write(buffer[i]);
  }
  delay(1000);
}
//미세먼지 받아오기//////////////////////////////////////////////
void read_pm(){
  //받아오기
    while ( status != WL_CONNECTED) {
    Serial.print("Attempting to connect to WPA SSID: ");
    Serial.println(ssid);
    // Connect to WPA/WPA2 network
    status = WiFi.begin(ssid, pass);
  }
    while (!client.connect(server, 80)) {
      Serial.print(F("."));
      delay(100);
    }
    Serial.println(F("CONNECTED"));
    
    client.println("GET /openapi/services/rest/ArpltnInforInqireSvc/getMsrstnAcctoRltmMesureDnsty?serviceKey=LgrUg9w0Kjz3Hp3c2InxlATULfE0bnkL7zjEP5dS9IcwJF18Cp2xph6hzsSSgzJzS19a0CI11KKyrrgrAj%2FtvQ%3D%3D&numOfRows=1&pageSize=1&pageNo=1&startPage=1&stationName=%EC%9A%A9%EC%95%94%EB%8F%99&dataTerm=DAILY&ver=1.3 HTTP/1.1");
    client.println("Host: openapi.airkorea.or.kr");
    client.println("Connection: close");
    client.println();

//타임아웃
   unsigned long timeout = millis();
  while (client.available() == 0) {
    if (millis() - timeout > 5000) {
      Serial.println(">>> Client Timeout !");
      client.stop();
      read_pm();
    }
  }
    
  if(client.available()){
    while (client.available()) {
    String line = client.readStringUntil('\n');
    int pm10 = line.indexOf("</pm10Value>");
    if(pm10>0) {
        String tmp_str="<pm10Value>";
        String wt_temp = line.substring(line.indexOf(tmp_str)+tmp_str.length(),pm10);
        
        Serial.print("PM10VALUE is : "); 
        if(wt_temp.equals("-")){
          Serial.println("-");
        }else{
         //-가 아니면 p_temp 에 pmValue 넣기
          pmValue = wt_temp.toInt();
          p_temp = pmValue;
           Serial.println(pmValue);  
        }
      }
    }
  }
}
