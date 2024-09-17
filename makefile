# Determine the platform

UNAME_S := $(shell uname -s)
ifeq ($(UNAME_S),Darwin)
    # macOS
    DEL = rm -f
    OPEN_TERMINAL = open -a Terminal .

else ifeq ($(UNAME_S),Linux)
    # Linux
    DEL = rm -f
    OPEN_TERMINAL = open -a Terminal .
else
    # Windows
    DEL = del
    OPEN_TERMINAL = start cmd
endif

JAVAC = javac
JAVA = java
CP = -cp ".;lib/gson-2.11.0.jar"

MAIN = main

BIN_DIR = bin

all: compile

compile: 
		$(JAVAC) $(CP) -d $(BIN_DIR) src/AggregationServer.java src/ContentServer.java src/GETClient.java

runaggserver: 
		$(OPEN_TERMINAL) && cd $(BIN_DIR) && $(JAVA) $(CP) AggregationServer

runcntserver: 
		$(OPEN_TERMINAL) && cd $(BIN_DIR) && $(JAVA) $(CP) ContentServer http://localhost:4567 weather_data.txt

runclient:
		cd $(BIN_DIR) && $(JAVA) $(CP) GETClient http://localhost:4567

clean: 
		cd $(BIN_DIR) && $(DEL) *.class