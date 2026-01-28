#!/bin/bash

# ==============================================
# LegalPay - Quick Start Script
# ==============================================
# This script starts both backend and frontend in development mode

set -e  # Exit on error

echo "🚀 Starting LegalPay Platform..."

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check prerequisites
echo -e "${BLUE}Checking prerequisites...${NC}"

if ! command -v java &> /dev/null; then
    echo "❌ Java not found. Please install Java 21"
    exit 1
fi

if ! command -v mvn &> /dev/null; then
    echo "❌ Maven not found. Please install Maven 3.9+"
    exit 1
fi

if ! command -v node &> /dev/null; then
    echo "❌ Node.js not found. Please install Node.js 20"
    exit 1
fi

echo -e "${GREEN}✅ Prerequisites OK${NC}"

# Build backend (if not already built)
if [ ! -d "legalpay-api/target" ]; then
    echo -e "${BLUE}Building backend (first time)...${NC}"
    mvn clean install -DskipTests
    echo -e "${GREEN}✅ Backend built${NC}"
fi

# Install frontend dependencies (if not already installed)
if [ ! -d "frontend/node_modules" ]; then
    echo -e "${BLUE}Installing frontend dependencies...${NC}"
    cd frontend
    npm install
    cd ..
    echo -e "${GREEN}✅ Frontend dependencies installed${NC}"
fi

# Start backend in background
echo -e "${BLUE}Starting backend on port 8080...${NC}"
cd legalpay-api
mvn spring-boot:run > ../logs/backend.log 2>&1 &
BACKEND_PID=$!
cd ..

# Wait for backend to start
echo "Waiting for backend to start..."
for i in {1..30}; do
    if curl -s http://localhost:8080/health > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Backend started (PID: $BACKEND_PID)${NC}"
        break
    fi
    sleep 1
    echo -n "."
done

# Start frontend in background
echo -e "${BLUE}Starting frontend on port 3000...${NC}"
cd frontend
npm run dev > ../logs/frontend.log 2>&1 &
FRONTEND_PID=$!
cd ..

echo -e "${GREEN}✅ Frontend started (PID: $FRONTEND_PID)${NC}"

# Save PIDs for cleanup
echo $BACKEND_PID > .backend.pid
echo $FRONTEND_PID > .frontend.pid

echo ""
echo "========================================="
echo -e "${GREEN}✨ LegalPay is running!${NC}"
echo "========================================="
echo ""
echo "Frontend:  http://localhost:3000"
echo "Backend:   http://localhost:8080"
echo "API Docs:  http://localhost:8080/swagger-ui.html"
echo "H2 Console: http://localhost:8080/h2-console"
echo ""
echo "Logs:"
echo "  Backend:  tail -f logs/backend.log"
echo "  Frontend: tail -f logs/frontend.log"
echo ""
echo "To stop: ./scripts/stop.sh"
echo "========================================="
