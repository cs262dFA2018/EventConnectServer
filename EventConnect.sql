DROP TABLE IF EXISTS JoinedEvents;
DROP TABLE IF EXISTS Events;
DROP TABLE IF EXISTS Users;

CREATE TABLE Users (
	ID integer PRIMARY KEY,
	Username Varchar(20) NOT NULL UNIQUE,
	Password Varchar(50) NOT NULL
);

CREATE TABLE Events(
	Id integer PRIMARY KEY,
	UserID integer REFERENCES Users(ID),
	Title Varchar(25) NOT NULL, 
	Description Varchar(250) NOT NULL,
	Time timestamp,
	Location Varchar(150),
	Cost numeric, 
	Threshold integer,
	Capacity integer,
	Category Varchar(20)
);

CREATE TABLE JoinedEvents(
	EventID integer REFERENCES Events(ID) ON DELETE CASCADE,
	UserId integer REFERENCES Users(ID) ON DELETE CASCADE
);

INSERT INTO Users VALUES (1, 'TestUser', 'TestPass');

INSERT INTO Events VALUES(1,1,'Example Event', 'Example Event Description',
	'2019-01-01 00:00:00', 'Example Location',
	0.00, 1, 10, 'ExampleCategory');

INSERT INTO JoinedEvents VALUES (1,1);
