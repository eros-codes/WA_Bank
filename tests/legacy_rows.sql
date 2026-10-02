-- rows exactly as the old code wrote them
INSERT INTO userinfo (username,password,fullname,id,birth,gender,marital,national,address,postalcode,phonenumber,email,occupation,employeraddress,annualincome,creationdate,accounttype,accountnumber,cardnumber,cvv,iban,expiredate,deposit,idphoto,statement) VALUES
 (1001,'pass1234','Sara Ahmadi','0011223344','1380/01/01','Female','Single','Iranian','Tabriz','5100000000','09120000001','s@x.ir','Engineer','Tabriz','100','2025-01-01','checking','1000000001','6037990000000001','123','IR000000000000000000000001','2030.05','1.5E7',x'00','Accepted'),
 (1002,'pass5678','Ali Rezaei','0011223355','1379/01/01','Male','Married','Iranian','Tabriz','5100000001','09120000002','a@x.ir','Teacher','Tabriz','100','2025-01-01','Checking','1000000002','6037990000000002','456','IR000000000000000000000002','2030.06','1,500,000',x'00','accepted'),
 (1001,'pass1234','Sara Ahmadi','0011223344','1380/01/01','Female','Single','Iranian','Tabriz','5100000000','09120000001','s@x.ir','Engineer','Tabriz','100','2025-02-01','saving','1000000003','6037990000000003','789','IR000000000000000000000003','2030.07','2500000.0',x'00','Accepted');
INSERT INTO loan (username,loanamount,term,intrest,date,id,employment,collateral,deed,status) VALUES
 (1001,'5000000.0','12','18','2025-01-02',x'00',x'00',x'00',x'00','pending'),
 (1002,'5000000.0','12','18','2025-01-02',x'00',x'00',x'00',x'00','pending');
INSERT INTO workerinfo (username,password,fullname) VALUES ('emp1','worker123','Reza Karimi');
