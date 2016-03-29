# GAMME Hive

![Hive](https://raw.githubusercontent.com/MelonSmasher/GAMME-Hive/master/hive.png)

A cluster tool that controls many drone machines, delegating them imap email addresses that need to be migrated using the `GAMME Tool`.


# Install

## Drone Install:

Install java8 and optionally ConEmu.

```shell
choco install javaruntime jdk8 -y
choco install conemu -y
```

##### In Powershell run:

```powershell
New-Item "C:\Program Files\GAMME Hive" -type directory;
(New-Object System.Net.WebClient).DownloadFile("https://github.com/MelonSmasher/GAMME-Hive/releases/download/v0.0.1/HiveDrone.jar", "C:\Program Files\GAMME Hive\HiveDrone.jar");
(New-Object System.Net.WebClient).DownloadFile("https://raw.githubusercontent.com/MelonSmasher/GAMME-Hive/v0.0.1/conf/drone.conf.example.json", "C:\Program Files\GAMME Hive\conf.json");
```

Then open: `"C:\Program Files\GAMME Hive\conf.json"` in a text editor and configure the drone.

## Queen Install:

This has only been tested on Debian and OS X, the directions will be for Debian.

#### Step 1: Download files

```shell
# Prerequisites:
apt-get install tmux wget mysql-server mysql-client libgl1-mesa-glx libfontconfig1 libxslt1.1 libxtst6 libxxf86vm1 libgtk2.0-0;
# Create install dir
mkdir -p /opt/hive;
# Create configuration dir
mkdir -p /etc/gamme_hive;
cd /opt/hive;
# Get launcher script
wget https://raw.githubusercontent.com/MelonSmasher/GAMME-Hive/v0.0.1/sbin/hive.sh;
# Download binary
wget https://github.com/MelonSmasher/GAMME-Hive/releases/download/v0.0.1/HiveQueen.jar;
chmod +x hive.sh;
ln -s /opt/hive/hive.sh /usr/sbin/hive;
# Download example conf:
cd /etc/gamme_hive;
wget https://raw.githubusercontent.com/MelonSmasher/GAMME-Hive/v0.0.1/conf/queen.conf.example.json -O conf.json;
```

#### Step 2: Install Java

This may work with the open JRE JDK, have not tried. Either way java 1.8 needs to be installed, here are directions for installing the oracle JRE on Debian: [https://wiki.debian.org/JavaPackage](https://wiki.debian.org/JavaPackage).

#### Step 3: Configure MySQL

Open a mysql command prompt:

```shell
mysql -u root -p
```

Create the database and user:

```mysql
CREATE DATABASE hive;
USE hive;
CREATE USER hive_user@localhost IDENTIFIED BY 'Your-Strong-Password';
GRANT ALL ON hive.* TO hive_user@localhost IDENTIFIED BY 'Your-Strong-Password';
FLUSH PRIVILEGES;
```

Paste the contents of this file: [https://raw.githubusercontent.com/MelonSmasher/GAMME-Hive/v0.0.1/conf/hive.sql](https://raw.githubusercontent.com/MelonSmasher/GAMME-Hive/v0.0.1/conf/hive.sql) into the MySQL prompt while using the `hive` database.

Optionally, it might be useful to have PHP My Admin insatlled:

```shell
apt-get install phpmyadmin -y
```

#### Step 4: Configure

Open the conf file and edit it to match your parameters.

```shell
vi /etc/gamme_hive/conf.json
```

You will need to place server lists, email lists, and your Google Auth token in the conf dir(`/etc/gamme_hive`).


---

# Run It

#### 1: Queen

```shell
tmux new -s hive;
hive; # or /usr/sbin/hive
```

The Queen should fire up.

#### 2: Drone

```cmd
cd C:\Program Files\GAMME Hive
java -jar HiveDrone.jar
```

The Drone will fire up and connect to the Queen.
