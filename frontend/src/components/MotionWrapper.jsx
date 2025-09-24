import React from 'react';
import { motion } from 'framer-motion';

/**
 * MotionWrapper provides a consistent fade/slide animation for page-level sections.
 */
export default function MotionWrapper({ children, delay = 0 }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -8 }}
      transition={{ duration: 0.45, ease: [0.4, 0.0, 0.2, 1], delay }}
      style={{ width: '100%' }}
    >
      {children}
    </motion.div>
  );
}
