# Swing-based Mulit-user Chat Application
Author: Ziyi Liang

## Table of Contents
-[Description](#Description)<br>
-[Dependencies](#Dependencies)<br>
-[Installation](#Installation)<br>
-[Quick start](#Quick-start)<br>
-[Files](#Files)<br>

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

│  .classpath<br>
│  .DS_Store<br>
│  .gitattributes<br>
│  .gitignore<br>
│  .project<br>
│  hamcrest-core-1.3.jar<br>
│  junit-4.13.2.jar<br>
│  README.md<br>
│  server.db<br>
│  sqlite-jdbc-3.34.0.jar<br>
│<br>
├─bin<br>
│  │  .DS_Store<br>
│  │  .gitignore<br>
│  │<br>
│  ├─chat<br>
│  │<br>
│  └─encryption<br>
│<br>
├─keypairs<br>
│      id_rsa<br>
│      id_rsa.pub<br>
│      pkcs8_key<br>
│<br>
├─sqlite-tools-win-x64-3450200<br>
│      sqldiff.exe<br>
│      sqlite3.exe<br>
│      sqlite3_analyzer.exe<br>
│<br>
├─src<br>
│  │  .DS_Store<br>
│  │<br>
│  ├─chat<br>
│  │      ChatClientController.java: The controller of chat client<br>
│  │      ChatClientModel.java: The logic of chat client<br>
│  │      ChatClientView.java: The UI of chat client<br>
│  │      ChatServerController.java: The controller of chat server<br>
│  │      ChatServerModel.java: The logic of chat server<br>
│  │      ChatServerView.java: The UI of chat server<br>
│  │      ServerDatabase.java: Implement CRUD functions<br>
│  │      SettingFrame.java: A windows of configuring account setting<br>
│  │      User.java: A simple user class<br>
│  │      WelcomeFrame.java: The login/register windows<br>
│  │<br>
│  └─encryption<br>
│          Encryption.java: Code provided by Professor Dean<br>
│<br>
├─target<br>
│  │  Intro-To-Java-Project-0.0.1-SNAPSHOT.jar<br>
│  │<br>
│  └─maven-archiver<br>
│          pom.properties<br>
│<br>
└─test<br>
    └─chat<br>
            ServerDatabaseTest.java: The test script of `ServerDatabase.java` <br>
