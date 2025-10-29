CREATE TABLE IF NOT EXISTS Interest_Rate_Master (
  Intt_Code VARCHAR(20) PRIMARY KEY,
  Intt_Rate DECIMAL(5,2) NOT NULL,
  Intt_Effctv_Date DATE NOT NULL
);

INSERT INTO Interest_Rate_Master (Intt_Code, Intt_Rate, Intt_Effctv_Date) VALUES
('Intt_Zero', 0.00, '2025-01-01') ON DUPLICATE KEY UPDATE Intt_Rate=VALUES(Intt_Rate), Intt_Effctv_Date=VALUES(Intt_Effctv_Date);

INSERT INTO Interest_Rate_Master (Intt_Code, Intt_Rate, Intt_Effctv_Date) VALUES
('Intt_SB', 6.00, '2025-01-01') ON DUPLICATE KEY UPDATE Intt_Rate=VALUES(Intt_Rate), Intt_Effctv_Date=VALUES(Intt_Effctv_Date);

INSERT INTO Interest_Rate_Master (Intt_Code, Intt_Rate, Intt_Effctv_Date) VALUES
('Intt_TD_30D', 9.00, '2025-01-01') ON DUPLICATE KEY UPDATE Intt_Rate=VALUES(Intt_Rate), Intt_Effctv_Date=VALUES(Intt_Effctv_Date);

INSERT INTO Interest_Rate_Master (Intt_Code, Intt_Rate, Intt_Effctv_Date) VALUES
('Intt_OD', 12.00, '2025-01-01') ON DUPLICATE KEY UPDATE Intt_Rate=VALUES(Intt_Rate), Intt_Effctv_Date=VALUES(Intt_Effctv_Date);


