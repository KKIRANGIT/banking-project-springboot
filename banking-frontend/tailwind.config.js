/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,jsx}"],
  theme: {
    extend: {
      colors: {
        ink: "#16221f",
        mist: "#eef2ec",
        clay: "#f4e6d2",
        ember: "#c75c2f",
        pine: "#21493f",
        lake: "#2e6f83",
        gold: "#d8a653"
      },
      boxShadow: {
        panel: "0 24px 80px rgba(18, 28, 25, 0.14)"
      },
      borderRadius: {
        "4xl": "2rem"
      },
      fontFamily: {
        display: ['"Space Grotesk"', "sans-serif"],
        body: ['"IBM Plex Sans"', "sans-serif"]
      }
    }
  },
  plugins: []
};
