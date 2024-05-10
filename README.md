# Swing-based Mulit-user Chat Application
Author: Ziyi Liang

## Table of Contents
-[Description](#Description)
-[Dependencies](#Dependencies)
-[Installation](#Installation)
-[Quick start](#Quick-start)
-[Files](#Files)

## Description
This project aims to develop a swing-based chat application featuring user registration, login/logout capabilities, change username/password, delete account, and chat with other users.

## Dependencies
All dependencies are already in the root directory:
sqlite-jdbc-3.34.0.jar
hamcrest-core-1.3.jar
junit-4.13.2.jar

## Installation
The project is compatible with JAVA 1.8.

## Quick start
To initiate a chat server, please run `ChatServerController.java`. To initate a chat client, please run `ChatClientController.java`. More usage please see demo video.

## Files

│  .classpath  
│  .DS_Store  
│  .gitattributes  
│  .gitignore  
│  .project  
│  hamcrest-core-1.3.jar  
│  junit-4.13.2.jar
│  README.md
│  server.db
│  sqlite-jdbc-3.34.0.jar
│
├─bin
│  │  .DS_Store
│  │  .gitignore
│  │
│  ├─chat
│  │
│  └─encryption
│
├─keypairs
│      id_rsa
│      id_rsa.pub
│      pkcs8_key
│
├─sqlite-tools-win-x64-3450200
│      sqldiff.exe
│      sqlite3.exe
│      sqlite3_analyzer.exe
│
├─src
│  │  .DS_Store
│  │
│  ├─chat
│  │      ChatClientController.java: The controller of chat client
│  │      ChatClientModel.java: The logic of chat client
│  │      ChatClientView.java: The UI of chat client
│  │      ChatServerController.java: The controller of chat server
│  │      ChatServerModel.java: The logic of chat server
│  │      ChatServerView.java: The UI of chat server
│  │      ServerDatabase.java: Implement CRUD functions
│  │      SettingFrame.java: A windows of configuring account setting
│  │      User.java: A simple user class
│  │      WelcomeFrame.java: The login/register windows
│  │
│  └─encryption
│          Encryption.java: Code provided by Professor Dean
│
├─target
│  │  Intro-To-Java-Project-0.0.1-SNAPSHOT.jar
│  │
│  └─maven-archiver
│          pom.properties
│
└─test
    └─chat
            ServerDatabaseTest.java: The test script of `ServerDatabase.java` 
