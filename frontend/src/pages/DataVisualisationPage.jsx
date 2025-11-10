import React from 'react';
import Heatmap from 'react-heatmap-grid';
import '../styles/Heatmap.css';

const TariffHeatmap = ({ data }) => {
  // Sample data structure
  const xLabels = ["USA", "China", "EU", "Japan"]; // Countries
  const yLabels = ["Electronics", "Agriculture", "Automotive"]; // Industries
  
  // Convert your data into a 2D array for the heatmap
  const data = [
    [0.4, 0.5, 0.3, 0.2],
    [0.2, 0.3, 0.5, 0.4],
    [0.6, 0.3, 0.4, 0.5]
  ];

  return (
    <div className="heatmap-container">
      <h2>Tariff Impact Heatmap</h2>
      <Heatmap
        xLabels={xLabels}
        yLabels={yLabels}
        data={data}
        cellStyle={(background, value, min, max) => ({
          background: `rgb(0, 151, 230, ${1 - (max - value) / (max - min)})`,
          fontSize: "11px",
        })}
        cellRender={value => value.toFixed(2)}
      />
    </div>
  );
};

export default TariffHeatmap;