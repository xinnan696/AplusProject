import { format, subDays, subWeeks, subMonths, subYears, eachDayOfInterval, eachWeekOfInterval, eachMonthOfInterval, startOfHour } from 'date-fns';

/**
 * Generates an array of string labels for a chart's X-axis based on a time range string.
 * This is primarily used for generating mock data when the backend is unavailable.
 * The real labels should come from the backend API.
 * @param range The time range string (e.g., '24 hours', 'one week').
 * @returns An array of string labels.
 */
export function generateXAxisLabels(range: string): string[] {
  const end = new Date();
  let start: Date;
  let xAxisLabels: string[] = [];

  switch (range) {
    case '24 hours':
      start = subDays(end, 1);
      for (let i = 0; i <= 24; i += 2) {
        const hour = startOfHour(new Date(start.getTime() + i * 3600 * 1000));
        xAxisLabels.push(format(hour, 'HH:00'));
      }
      break;
    case 'one week':
      start = subWeeks(end, 1);
      xAxisLabels = eachDayOfInterval({ start, end }).map((d: Date) => format(d, 'eee'));
      break;
    case 'one month':
      start = subMonths(end, 1);
      xAxisLabels = eachWeekOfInterval({ start, end }).map((d: Date, i: number) => `Week ${i + 1}`);
      break;
    case 'three months':
      start = subMonths(end, 3);
      xAxisLabels = eachMonthOfInterval({ start, end }).map((d: Date) => format(d, 'MMM'));
      break;
    case 'six months':
      start = subMonths(end, 6);
      xAxisLabels = eachMonthOfInterval({ start, end }).map((d: Date) => format(d, 'MMM'));
      break;
    case 'one year':
      start = subYears(end, 1);
      const months = eachMonthOfInterval({ start, end });
      for(let i = 0; i < months.length; i+=2) {
        xAxisLabels.push(format(months[i], 'MMM'));
      }
      break;
    default:
      xAxisLabels = ['Data Point 1', 'Data Point 2', 'Data Point 3', 'Data Point 4', 'Data Point 5'];
  }

  return xAxisLabels;
}
