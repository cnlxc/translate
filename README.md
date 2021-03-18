# translate
名前通り


strConOracle = "Driver={Microsoft ODBC for Oracle}; " & _
         "CONNECTSTRING=(DESCRIPTION=" & _
         "(ADDRESS=(PROTOCOL=TCP)" & _
         "(HOST=" & strHost & ")(PORT=1521))" & _
         "(CONNECT_DATA=(SERVICE_NAME=" & strDatabase & "))); uid=" & strUser & " ;pwd=" & strPassword & ";"
