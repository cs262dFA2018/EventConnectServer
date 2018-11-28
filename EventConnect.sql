DROP TABLE IF EXISTS JoinedEvents;
DROP TABLE IF EXISTS Events;
DROP TABLE IF EXISTS Users;

CREATE TABLE Users (
	ID integer PRIMARY KEY,
	EmailAddress Varchar (50) NOT NULL,
	UserName Varchar(20) NOT NULL,
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
	EventID integer REFERENCES Events(ID),
	UserId integer REFERENCES Users(ID)
);


INSERT INTO Users VALUES (1, 'abc@aol.com', 'alpha7',
	'AlphaBingCongo7');
INSERT INTO Users VALUES (2, 'cba@aol.com', 'beta2',
	'AlphaBingCongo8');
INSERT INTO Users VALUES (3, 'bca@aol.com', 'congo5',
	'AlphaBingCongo9');
INSERT INTO Users VALUES (4, 'acb@aol.com', 'delta8',
	'AlphaBingCongo3');

INSERT INTO Events VALUES(1,2,'HBD Joe', 'Tis Joe''s birthday',
	'2018-10-31 16:00:00', 'Calvin College', 
	0.00, 2, 30, 'Birthday');

INSERT INTO Events VALUES(2,1,'RIP Joe', 'Rest in peace Joe',
	'2020-10-31 14:35:00', 'McDonalds', 
	10.25,1,50, 'Funeral');

INSERT INTO Events VALUES(3,3,'Trump Baby Balloon', 
	'Join the world famous ''Baby Trump'' in Rosa Parks Circle on October 27th!',
	'2018-10-27 10:00:00', 'Rosa Park Circle', 
	0.00,1,50, 'Rally');

INSERT INTO JoinedEvents VALUES (1,1);
INSERT INTO JoinedEvents VALUES (1,2);
INSERT INTO JoinedEvents VALUES (1,3);
INSERT INTO JoinedEvents VALUES (2,2);
INSERT INTO JoinedEvents VALUES (3,2);





