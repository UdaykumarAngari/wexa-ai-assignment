import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import axios from 'axios'
import './index.css'
import App from './App.jsx'

axios.defaults.baseURL = import.meta.env.VITE_API_URL || 'http://localhost:8000';

// Prevent precision loss on 64-bit Long IDs in JavaScript
const bigintRegex = /(?<![\d"])(-?\d{16,})(?![\d"])/g;
axios.defaults.transformResponse = [
  (data) => {
    if (typeof data === 'string') {
      try {
        data = data.replace(bigintRegex, '"$1"');
      } catch (e) {
        console.error('BigInt regex parsing error:', e);
      }
    }
    try {
      return JSON.parse(data);
    } catch (err) {
      return data;
    }
  }
];

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
