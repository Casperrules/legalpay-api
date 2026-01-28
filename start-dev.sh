#!/bin/bash

# LegalPay Quick Start Script
# This script sets up the development environment and starts both backend and frontend

set -e

echo "🚀 LegalPay Quick Start"
echo "======================="

# Check if .env exists
if [ ! -f .env ]; then
    echo "⚠️  .env file not found. Creating from template..."
    cp .env.example .env
    echo "✅ Created .env - Please update with your Razorpay credentials"
    echo "   Edit .env and add:"
    echo "   - RAZORPAY_KEY_ID"
    echo "   - RAZORPAY_KEY_SECRET"
    echo "   - RAZORPAY_WEBHOOK_SECRET"
    echo ""
    read -p "Press Enter after updating .env to continue..."
fi

# Check if frontend/.env exists
if [ ! -f frontend/.env ]; then
    echo "⚠️  frontend/.env not found. Creating from template..."
    cp frontend/.env.example frontend/.env
    echo "✅ Created frontend/.env"
fi

# Build backend
echo ""
echo "📦 Building backend..."
mvn clean install -DskipTests -q

if [ $? -ne 0 ]; then
    echo "❌ Backend build failed"
    exit 1
fi
echo "✅ Backend built successfully"

# Install frontend dependencies
echo ""
echo "📦 Installing frontend dependencies..."
cd frontend
npm install --silent

if [ $? -ne 0 ]; then
    echo "❌ Frontend dependency installation failed"
    exit 1
fi
echo "✅ Frontend dependencies installed"

cd ..

# Start services
echo ""
echo "🎯 Starting services..."
echo ""
echo "Backend will start on:  http://localhost:8080"
echo "Frontend will start on: http://localhost:3000"
echo ""
echo "Press Ctrl+C to stop all services"
echo ""

# Start backend in background
echo "Starting backend..."
cd legalpay-api
mvn spring-boot:run > ../logs/backend.log 2>&1 &
BACKEND_PID=$!
cd ..

# Wait for backend to start
echo "Waiting for backend to start..."
sleep 10

# Start frontend in background
echo "Starting frontend..."
cd frontend
npm run dev > ../logs/frontend.log 2>&1 &
FRONTEND_PID=$!
cd ..

echo ""
echo "✅ All services started!"
echo ""
echo "📊 Service Status:"
echo "   Backend PID:  $BACKEND_PID"
echo "   Frontend PID: $FRONTEND_PID"
echo ""
echo "📝 Logs:"
echo "   Backend:  tail -f logs/backend.log"
echo "   Frontend: tail -f logs/frontend.log"
echo ""
echo "🌐 Open your browser:"
echo "   http://localhost:3000"
echo ""
echo "🛑 To stop services:"
echo "   kill $BACKEND_PID $FRONTEND_PID"
echo ""

# Save PIDs to file
echo $BACKEND_PID > .backend.pid
echo $FRONTEND_PID > .frontend.pid

# Wait for Ctrl+C
trap "echo ''; echo '🛑 Stopping services...'; kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; rm -f .backend.pid .frontend.pid; echo '✅ Services stopped'; exit 0" INT

# Keep script running
wait
